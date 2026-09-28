package com.tekwatt.auth.repository;

import com.tekwatt.auth.entity.PasswordResetCode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, UUID> {
    Optional<PasswordResetCode> findByUser_Id(UUID userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select code from PasswordResetCode code where code.user.id = :userId")
    Optional<PasswordResetCode> findLockedByUserId(@Param("userId") UUID userId);
}
