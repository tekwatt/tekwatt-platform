package com.tekwatt.organization.repository;
import com.tekwatt.organization.entity.Organization;import java.util.UUID;import org.springframework.data.domain.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface OrganizationRepository extends JpaRepository<Organization,UUID>{Page<Organization> findByTenantId(UUID tenantId,Pageable pageable);boolean existsByTenantIdAndCodeIgnoreCase(UUID tenantId,String code);boolean existsByTenantIdAndCodeIgnoreCaseAndIdNot(UUID tenantId,String code,UUID id);}
