package com.tekwatt.auth.repository;
import com.tekwatt.auth.entity.AppUser;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface AppUserRepository extends JpaRepository<AppUser, UUID> {
    Optional<AppUser> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    Optional<AppUser> findByVerifiedPhone(String verifiedPhone);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from AppUser user where lower(user.email) = lower(:email)")
    Optional<AppUser> findForReset(@Param("email") String email);
}
