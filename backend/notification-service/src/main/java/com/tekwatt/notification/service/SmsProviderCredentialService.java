package com.tekwatt.notification.service;

import com.tekwatt.notification.entity.SmsProviderCredential;
import com.tekwatt.notification.repository.SmsProviderCredentialRepository;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SmsProviderCredentialService {
    private final SmsProviderCredentialRepository repository;
    private final String encodedKey;
    private final SecureRandom random = new SecureRandom();

    public SmsProviderCredentialService(SmsProviderCredentialRepository repository,
            @Value("${tekwatt.sms.credentials-encryption-key:}") String encodedKey) {
        this.repository = repository;
        this.encodedKey = encodedKey;
    }

    @Transactional(readOnly = true)
    public List<Summary> list(UUID tenantId) {
        return repository.findAllByTenantId(tenantId).stream().map(item -> new Summary(
                item.getProvider(), true, item.getPublicIdentifier(), item.getSender(),
                item.getTemplateId(), item.getChargingStartedTemplateId(), item.getChargingCompletedTemplateId(),
                item.getMessageVariable(), item.getUpdatedAt())).toList();
    }

    @Transactional
    public Summary save(UUID tenantId, Request request) {
        String provider = request.provider() == null ? "" : request.provider().trim().toUpperCase();
        if (!provider.equals("MSG91") && !provider.equals("TWILIO")) bad("Select MSG91 or Twilio.");
        String identifier = trimmed(request.publicIdentifier());
        String sender = trimmed(request.sender());
        String template = trimmed(request.templateId());
        String chargingStartedTemplate = trimmed(request.chargingStartedTemplateId());
        String chargingCompletedTemplate = trimmed(request.chargingCompletedTemplateId());
        String variable = trimmed(request.messageVariable());
        if (provider.equals("MSG91") && template.isBlank() && chargingStartedTemplate.isBlank() && chargingCompletedTemplate.isBlank())
            bad("Enter at least one MSG91 Flow template ID.");
        if (provider.equals("TWILIO") && (!identifier.matches("AC[0-9a-fA-F]{32}") || sender.isBlank()))
            bad("Enter a valid Twilio account SID and sender number.");
        if (provider.equals("MSG91")) { identifier = ""; sender = ""; variable = variable.isBlank() ? "message" : variable; }
        else { template = ""; chargingStartedTemplate = ""; chargingCompletedTemplate = ""; variable = ""; }
        Optional<SmsProviderCredential> prior = repository.findByTenantIdAndProvider(tenantId, provider);
        String secret = request.secret() == null ? "" : request.secret().trim();
        if (secret.isBlank() && prior.isEmpty()) bad("Provider key is required when adding a provider.");
        if (secret.isBlank()) secret = decrypt(tenantId, provider, prior.orElseThrow().getEncryptedSecret());
        SmsProviderCredential item = prior.orElseGet(() -> new SmsProviderCredential(tenantId, provider));
        item.update(identifier, sender, template, chargingStartedTemplate, chargingCompletedTemplate, variable,
                encrypt(tenantId, provider, secret));
        repository.save(item);
        return new Summary(provider, true, identifier, sender, template, chargingStartedTemplate,
                chargingCompletedTemplate, variable, item.getUpdatedAt());
    }

    @Transactional(readOnly = true)
    public Optional<Credentials> credentials(UUID tenantId, String provider) {
        return repository.findByTenantIdAndProvider(tenantId, provider).map(item -> new Credentials(
                item.getPublicIdentifier(), item.getSender(), item.getTemplateId(),
                item.getChargingStartedTemplateId(), item.getChargingCompletedTemplateId(), item.getMessageVariable(),
                decrypt(tenantId, provider, item.getEncryptedSecret())));
    }

    private String encrypt(UUID tenantId, String provider, String plain) {
        try {
            byte[] iv = new byte[12]; random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            cipher.updateAAD((tenantId + ":" + provider).getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] stored = Arrays.copyOf(iv, iv.length + ciphertext.length);
            System.arraycopy(ciphertext, 0, stored, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(stored);
        } catch (ResponseStatusException exception) { throw exception;
        } catch (Exception exception) { throw new IllegalStateException("SMS provider credentials could not be stored."); }
    }

    private String decrypt(UUID tenantId, String provider, String stored) {
        try {
            byte[] bytes = Base64.getDecoder().decode(stored);
            if (bytes.length < 29) throw new IllegalArgumentException("Invalid ciphertext");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, bytes, 0, 12));
            cipher.updateAAD((tenantId + ":" + provider).getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8);
        } catch (ResponseStatusException exception) { throw exception;
        } catch (Exception exception) { throw new IllegalStateException("SMS provider credentials could not be read."); }
    }

    private SecretKeySpec key() {
        try {
            byte[] raw = Base64.getDecoder().decode(encodedKey);
            if (raw.length != 32) throw new IllegalArgumentException("Invalid key length");
            return new SecretKeySpec(raw, "AES");
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "SMS credential storage needs a server encryption key. Ask the platform administrator to configure it.");
        }
    }

    private String trimmed(String value) { return value == null ? "" : value.trim(); }
    private void bad(String reason) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, reason); }
    public record Request(String provider, String publicIdentifier, String sender, String templateId,
                          String chargingStartedTemplateId, String chargingCompletedTemplateId,
                          String messageVariable, String secret) {}
    public record Summary(String provider, boolean secretConfigured, String publicIdentifier, String sender,
                          String templateId, String chargingStartedTemplateId, String chargingCompletedTemplateId,
                          String messageVariable, Instant updatedAt) {}
    public record Credentials(String publicIdentifier, String sender, String templateId,
                              String chargingStartedTemplateId, String chargingCompletedTemplateId,
                              String messageVariable, String secret) {}
}
