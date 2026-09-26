package com.tekwatt.notification.service;

import com.tekwatt.notification.dto.*;
import com.tekwatt.notification.entity.*;
import com.tekwatt.notification.repository.NotificationRepository;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class NotificationService {
    private final NotificationRepository repo;
    private final Msg91SmsClient sms;
    private final SmtpEmailClient email;

    public NotificationService(NotificationRepository repo, Msg91SmsClient sms, SmtpEmailClient email) {
        this.repo = repo;
        this.sms = sms;
        this.email = email;
    }

    public NotificationResponse create(CreateNotificationRequest request) {
        Optional<Notification> existing = repo.findByIdempotencyKey(request.idempotencyKey());
        if (existing.isPresent()) return NotificationResponse.from(existing.get());
        return NotificationResponse.from(repo.save(new Notification(request.tenantId(), request.userId(),
                request.idempotencyKey(), request.channel(), request.recipient(), request.subject(), request.body(),
                request.templateKey(), request.maxAttempts() == null ? 3 : request.maxAttempts())));
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(UUID id) { return NotificationResponse.from(find(id)); }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(UUID tenantId) {
        return repo.findAllByTenantIdOrderByCreatedAtDesc(tenantId).stream().map(NotificationResponse::from).toList();
    }

    public NotificationResponse send(UUID id) {
        Notification notification = repo.findForDelivery(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (notification.getStatus() == NotificationStatus.SENT) return NotificationResponse.from(notification);
        if (notification.getStatus() != NotificationStatus.QUEUED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Notification is not queued");
        try {
            notification.sending();
        } catch (IllegalStateException exception) {
            bad(exception);
        }
        try {
            if (notification.getChannel() == NotificationChannel.SMS)
                notification.sent(sms.send(notification.getRecipient(), notification.getBody()));
            else if (notification.getChannel() == NotificationChannel.EMAIL)
                notification.sent(email.send(notification.getRecipient(), notification.getSubject(), notification.getBody()));
            else
                notification.failed("Push delivery is not configured for this deployment.");
        } catch (Msg91SmsClient.SmsDeliveryException | SmtpEmailClient.EmailDeliveryException exception) {
            notification.failed(exception.getMessage());
        }
        return NotificationResponse.from(notification);
    }

    public NotificationResponse result(UUID id, DeliveryResultRequest request) {
        Notification notification = find(id);
        if (notification.getStatus() != NotificationStatus.SENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Notification is not being sent");
        if (request.successful()) notification.sent(request.providerMessageId());
        else notification.failed(request.error());
        return NotificationResponse.from(notification);
    }

    public NotificationResponse retry(UUID id) {
        Notification notification = find(id);
        if (notification.getStatus() != NotificationStatus.FAILED)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only failed notifications can be retried");
        try {
            notification.retry();
        } catch (IllegalStateException exception) {
            bad(exception);
        }
        return NotificationResponse.from(notification);
    }

    private void bad(IllegalStateException exception) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
    }

    private Notification find(UUID id) {
        return repo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }
}
