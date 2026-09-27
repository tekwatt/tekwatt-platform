package com.tekwatt.notification.repository;

import com.tekwatt.notification.entity.SmsProviderCredential;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SmsProviderCredentialRepository extends JpaRepository<SmsProviderCredential, UUID> {
    Optional<SmsProviderCredential> findByTenantIdAndProvider(UUID tenantId, String provider);
    List<SmsProviderCredential> findAllByTenantId(UUID tenantId);
}
