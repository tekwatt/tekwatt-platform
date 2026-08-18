package com.tekwatt.auth.repository;
import com.tekwatt.auth.entity.RefreshToken;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByToken(String token);
    List<RefreshToken> findAllByUser_IdOrderByCreatedAtDesc(UUID userId);
    Optional<RefreshToken> findByIdAndUser_Id(UUID id, UUID userId);
}
