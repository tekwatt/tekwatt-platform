package com.tekwatt.notification;

import com.tekwatt.notification.entity.SmsProviderCredential;
import com.tekwatt.notification.repository.SmsProviderCredentialRepository;
import com.tekwatt.notification.service.SmsProviderCredentialService;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SmsProviderCredentialServiceTest {
    @Test void savesArbitraryEventTemplatesAndReadsThemBack() {
        UUID tenant = UUID.randomUUID();
        var repository = mock(SmsProviderCredentialRepository.class);
        var stored = new AtomicReference<SmsProviderCredential>();
        when(repository.findByTenantIdAndProvider(tenant, "MSG91")).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(repository.save(any(SmsProviderCredential.class))).thenAnswer(invocation -> {
            var item = invocation.getArgument(0, SmsProviderCredential.class); stored.set(item); return item;
        });
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        var service = new SmsProviderCredentialService(repository, key);
        var templates = java.util.List.of(
                new SmsProviderCredentialService.FlowTemplate("charging-started", "start-id", "message"),
                new SmsProviderCredentialService.FlowTemplate("charging-completed", "stop-id", "message"),
                new SmsProviderCredentialService.FlowTemplate("maintenance-alert", "alert-id", "alert_text"));
        var saved = service.save(tenant, new SmsProviderCredentialService.Request("MSG91", "", "", "", "", "", "", templates, "private-key"));
        assertThat(saved.flowTemplates()).containsExactlyElementsOf(templates);
        assertThat(service.credentials(tenant, "MSG91").orElseThrow().flowTemplates()).containsExactlyElementsOf(templates);
    }
    @Test void savesEncryptedSecretAndNeverReturnsItInSummary() {
        UUID tenant = UUID.randomUUID();
        var repository = mock(SmsProviderCredentialRepository.class);
        var stored = new AtomicReference<SmsProviderCredential>();
        when(repository.findByTenantIdAndProvider(tenant, "MSG91")).thenAnswer(invocation -> Optional.ofNullable(stored.get()));
        when(repository.save(any(SmsProviderCredential.class))).thenAnswer(invocation -> {
            var item = invocation.getArgument(0, SmsProviderCredential.class);
            stored.set(item);
            return item;
        });
        String key = Base64.getEncoder().encodeToString(new byte[32]);
        var service = new SmsProviderCredentialService(repository, key);
        var summary = service.save(tenant, new SmsProviderCredentialService.Request("MSG91", "", "", "template-1",
                "start-flow", "complete-flow", "message", null, "private-auth-key"));
        assertThat(summary.secretConfigured()).isTrue();
        assertThat(summary.toString()).doesNotContain("private-auth-key");
        assertThat(stored.get().getEncryptedSecret()).doesNotContain("private-auth-key");
        assertThat(summary.chargingStartedTemplateId()).isEqualTo("start-flow");
        assertThat(summary.chargingCompletedTemplateId()).isEqualTo("complete-flow");
        assertThat(service.credentials(tenant, "MSG91").orElseThrow().secret()).isEqualTo("private-auth-key");
    }

    @Test void refusesNewSecretWhenEncryptionKeyIsMissing() {
        var service = new SmsProviderCredentialService(mock(SmsProviderCredentialRepository.class), "");
        assertThatThrownBy(() -> service.save(UUID.randomUUID(),
                new SmsProviderCredentialService.Request("MSG91", "", "", "template-1", "", "", "message", null, "secret")))
                .isInstanceOf(ResponseStatusException.class);
    }
}
