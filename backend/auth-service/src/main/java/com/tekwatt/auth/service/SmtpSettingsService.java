package com.tekwatt.auth.service;

import com.tekwatt.auth.dto.SmtpSettingsRequest;
import com.tekwatt.auth.entity.SmtpSettings;
import com.tekwatt.auth.repository.SmtpSettingsRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SmtpSettingsService {
    private final SmtpSettingsRepository repository;
    private final ObjectProvider<JavaMailSender> environmentSender;
    private final String encryptionKey;
    private final String environmentHost;
    private final String environmentFrom;
    private final String environmentUsername;
    private final String environmentPassword;
    private final int environmentPort;
    private final SecureRandom random = new SecureRandom();

    public SmtpSettingsService(SmtpSettingsRepository repository, ObjectProvider<JavaMailSender> environmentSender,
            @Value("${tekwatt.auth.smtp-encryption-key:}") String encryptionKey,
            @Value("${spring.mail.host:}") String environmentHost,
            @Value("${tekwatt.auth.reset-from:}") String environmentFrom,
            @Value("${spring.mail.username:}") String environmentUsername,
            @Value("${spring.mail.password:}") String environmentPassword,
            @Value("${spring.mail.port:587}") int environmentPort) {
        this.repository = repository; this.environmentSender = environmentSender;
        this.encryptionKey = encryptionKey; this.environmentHost = environmentHost;
        this.environmentFrom = environmentFrom; this.environmentUsername = environmentUsername;
        this.environmentPassword = environmentPassword; this.environmentPort = environmentPort;
    }

    @Transactional(readOnly = true)
    public Summary status() {
        return repository.findById(1).map(row -> new Summary(true, "DATABASE", row.getHost(), row.getPort(),
                row.getSecurityMode(), row.getUsername(), row.getFromEmail(), row.getReplyTo(), true, row.getUpdatedAt()))
                .orElseGet(() -> new Summary(!environmentHost.isBlank() && !environmentFrom.isBlank(),
                        environmentHost.isBlank() ? "NONE" : "ENVIRONMENT", environmentHost,
                        environmentPort, "STARTTLS", environmentUsername, environmentFrom, "",
                        !environmentPassword.isBlank(), null));
    }

    @Transactional
    public Summary save(SmtpSettingsRequest request) {
        String host = request.host().trim();
        String mode = request.securityMode().trim().toUpperCase();
        if (!host.matches("(?i)[a-z0-9][a-z0-9.-]{0,253}[a-z0-9]") || host.contains(".."))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid SMTP server hostname.");
        if (!mode.equals("STARTTLS") && !mode.equals("SSL"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose STARTTLS or SSL/TLS for SMTP.");
        SmtpSettings prior = repository.findById(1).orElse(null);
        String password = request.password() == null ? "" : request.password();
        if (password.isBlank() && prior == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter the SMTP password or app-specific key.");
        if (password.length() > 1024)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SMTP password is too long.");
        String encrypted = password.isBlank() ? prior.getEncryptedPassword() : encrypt(password);
        String replyTo = request.replyTo() == null || request.replyTo().isBlank() ? null : request.replyTo().trim();
        if (prior == null) repository.save(new SmtpSettings(host, request.port(), mode,
                request.username().trim(), encrypted, request.fromEmail().trim(), replyTo));
        else prior.update(host, request.port(), mode, request.username().trim(), encrypted,
                request.fromEmail().trim(), replyTo);
        return status();
    }

    public void send(String recipient, String subject, String body) {
        SmtpSettings stored = repository.findById(1).orElse(null);
        JavaMailSender sender;
        String from;
        String replyTo = null;
        if (stored != null) {
            JavaMailSenderImpl configured = new JavaMailSenderImpl();
            configured.setHost(stored.getHost()); configured.setPort(stored.getPort());
            configured.setUsername(stored.getUsername());
            configured.setPassword(decrypt(stored.getEncryptedPassword()));
            configured.setProtocol("smtp");
            var properties = configured.getJavaMailProperties();
            properties.put("mail.smtp.auth", "true");
            properties.put("mail.smtp.ssl.enable", String.valueOf("SSL".equals(stored.getSecurityMode())));
            properties.put("mail.smtp.starttls.enable", String.valueOf("STARTTLS".equals(stored.getSecurityMode())));
            properties.put("mail.smtp.starttls.required", String.valueOf("STARTTLS".equals(stored.getSecurityMode())));
            properties.put("mail.smtp.ssl.checkserveridentity", "true");
            properties.put("mail.smtp.connectiontimeout", "5000");
            properties.put("mail.smtp.timeout", "5000");
            properties.put("mail.smtp.writetimeout", "5000");
            sender = configured; from = stored.getFromEmail(); replyTo = stored.getReplyTo();
        } else {
            if (environmentHost.isBlank() || environmentFrom.isBlank() || environmentSender.getIfAvailable() == null)
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Email delivery is not configured.");
            sender = environmentSender.getObject(); from = environmentFrom;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from); message.setTo(recipient); message.setSubject(subject); message.setText(body);
        if (replyTo != null) message.setReplyTo(replyTo);
        sender.send(message);
    }

    public void sendTest(String recipient) {
        try { send(recipient, "TekWatt email configuration test", "TekWatt SMTP is configured and able to send email."); }
        catch (ResponseStatusException failure) { throw failure; }
        catch (RuntimeException failure) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "SMTP test failed. Check server, port, encryption, sender and credentials.");
        }
    }

    private String encrypt(String plain) {
        try {
            byte[] iv = new byte[12]; random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            cipher.updateAAD("tekwatt.smtp.v1".getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] result = Arrays.copyOf(iv, iv.length + ciphertext.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (ResponseStatusException failure) { throw failure; }
        catch (Exception failure) { throw new IllegalStateException("SMTP credential could not be stored."); }
    }

    private String decrypt(String stored) {
        try {
            byte[] input = Base64.getDecoder().decode(stored);
            if (input.length < 29) throw new IllegalArgumentException("Invalid encrypted value");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, input, 0, 12));
            cipher.updateAAD("tekwatt.smtp.v1".getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(input, 12, input.length - 12), StandardCharsets.UTF_8);
        } catch (ResponseStatusException failure) { throw failure; }
        catch (Exception failure) { throw new IllegalStateException("Stored SMTP credential could not be read."); }
    }

    private SecretKeySpec key() {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptionKey);
            if (decoded.length != 32) throw new IllegalArgumentException("Expected a 256-bit key");
            return new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException failure) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "SMTP credential storage needs a server encryption key. Ask the platform administrator to configure it.");
        }
    }

    public record Summary(boolean configured, String source, String host, int port, String securityMode,
                          String username, String fromEmail, String replyTo, boolean passwordConfigured,
                          Instant updatedAt) { }
}
