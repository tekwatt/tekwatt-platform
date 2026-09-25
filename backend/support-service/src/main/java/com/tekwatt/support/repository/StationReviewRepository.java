package com.tekwatt.support.repository;

import com.tekwatt.support.entity.StationReview;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationReviewRepository extends JpaRepository<StationReview, UUID> {
    List<StationReview> findAllByTenantIdOrderByCreatedAtDesc(UUID tenantId);
}
