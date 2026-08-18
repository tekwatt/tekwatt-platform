package com.tekwatt.admin.repository;
import com.tekwatt.admin.entity.ModuleInstallation;import java.util.*;import org.springframework.data.jpa.repository.JpaRepository;
public interface ModuleInstallationRepository extends JpaRepository<ModuleInstallation,UUID>{List<ModuleInstallation>findAllByTenantId(UUID tenantId);Optional<ModuleInstallation>findByTenantIdAndModuleKey(UUID tenantId,String moduleKey);}
