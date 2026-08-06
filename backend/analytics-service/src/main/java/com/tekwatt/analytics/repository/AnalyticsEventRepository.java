package com.tekwatt.analytics.repository;
import com.tekwatt.analytics.entity.AnalyticsEvent;
import java.time.Instant;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent,UUID>{boolean existsByExternalEventId(String id);@Query(value="SELECT COUNT(*),COALESCE(SUM(energy_kwh),0),COALESCE(SUM(revenue),0),COALESCE(AVG(duration_seconds),0),COUNT(DISTINCT charger_id) FROM analytics_events WHERE tenant_id=:tenantId AND occurred_at>=:fromTime AND occurred_at<:toTime",nativeQuery=true)Object[] overview(@Param("tenantId")UUID tenantId,@Param("fromTime")Instant from,@Param("toTime")Instant to);@Query(value="SELECT DATE(occurred_at),COUNT(*),COALESCE(SUM(energy_kwh),0),COALESCE(SUM(revenue),0),COALESCE(AVG(duration_seconds),0),COUNT(DISTINCT charger_id) FROM analytics_events WHERE tenant_id=:tenantId AND occurred_at>=:fromTime AND occurred_at<:toTime GROUP BY DATE(occurred_at) ORDER BY DATE(occurred_at)",nativeQuery=true)List<Object[]> daily(@Param("tenantId")UUID tenantId,@Param("fromTime")Instant from,@Param("toTime")Instant to);}
