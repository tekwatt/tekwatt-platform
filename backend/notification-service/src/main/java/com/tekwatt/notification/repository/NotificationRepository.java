package com.tekwatt.notification.repository;
import com.tekwatt.notification.entity.Notification;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface NotificationRepository extends JpaRepository<Notification,UUID> {
    Optional<Notification> findByIdempotencyKey(String key);
    List<Notification> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from Notification n where n.id = :id")
    Optional<Notification> findForDelivery(UUID id);
}
