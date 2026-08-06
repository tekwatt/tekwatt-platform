package com.tekwatt.tenant.repository;
import com.tekwatt.tenant.entity.Tenant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
public interface TenantRepository extends JpaRepository<Tenant,UUID>{boolean existsBySlugIgnoreCase(String slug);boolean existsBySlugIgnoreCaseAndIdNot(String slug,UUID id);}
