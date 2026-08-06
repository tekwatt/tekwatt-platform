package com.tekwatt.tenant.service;
import com.tekwatt.tenant.dto.*;
import com.tekwatt.tenant.entity.Tenant;
import com.tekwatt.tenant.repository.TenantRepository;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
@Service
public class TenantService {
 private final TenantRepository repository;
 public TenantService(TenantRepository repository){this.repository=repository;}
 @Transactional public TenantResponse create(TenantRequest r){String slug=r.slug().toLowerCase();if(repository.existsBySlugIgnoreCase(slug))throw new ResponseStatusException(HttpStatus.CONFLICT,"Tenant slug already exists");return TenantResponse.from(repository.save(new Tenant(r.name().trim(),slug,r.contactEmail().trim().toLowerCase())));}
 @Transactional(readOnly=true) public TenantResponse get(UUID id){return TenantResponse.from(find(id));}
 @Transactional(readOnly=true) public Page<TenantResponse> list(Pageable pageable){return repository.findAll(pageable).map(TenantResponse::from);}
 @Transactional public TenantResponse update(UUID id,TenantRequest r){Tenant t=find(id);String slug=r.slug().toLowerCase();if(repository.existsBySlugIgnoreCaseAndIdNot(slug,id))throw new ResponseStatusException(HttpStatus.CONFLICT,"Tenant slug already exists");t.update(r.name().trim(),slug,r.contactEmail().trim().toLowerCase());return TenantResponse.from(t);}
 @Transactional public TenantResponse changeStatus(UUID id,TenantStatusRequest r){Tenant t=find(id);t.setStatus(r.status());return TenantResponse.from(t);}
 private Tenant find(UUID id){return repository.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Tenant not found"));}
}
