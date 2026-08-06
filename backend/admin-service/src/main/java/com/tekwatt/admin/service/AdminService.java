package com.tekwatt.admin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.admin.dto.AdminActionRequest;
import com.tekwatt.admin.dto.AdminActionResponse;
import com.tekwatt.admin.entity.AdminAction;
import com.tekwatt.admin.repository.AdminActionRepository;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AdminService {
  private final AdminActionRepository repository;
  private final RestClient tenant;
  private final RestClient user;
  private final RestClient charger;
  private final RestClient ocpp;

  public AdminService(
      AdminActionRepository repository,
      @Qualifier("tenantClient") RestClient tenant,
      @Qualifier("userClient") RestClient user,
      @Qualifier("chargerClient") RestClient charger,
      @Qualifier("ocppClient") RestClient ocpp) {
    this.repository = repository;
    this.tenant = tenant;
    this.user = user;
    this.charger = charger;
    this.ocpp = ocpp;
  }

  public JsonNode tenants(int page, int size) {
    return get(tenant, "/api/v1/tenants?page=" + page + "&size=" + size);
  }

  public JsonNode users(UUID tenantId, int page, int size) {
    return get(user, "/api/v1/users?tenantId=" + tenantId + "&page=" + page + "&size=" + size);
  }

  public JsonNode chargers(UUID tenantId) {
    return get(charger, "/api/v1/chargers?tenantId=" + tenantId);
  }

  public JsonNode connections() {
    return get(ocpp, "/api/v1/ocpp/connections");
  }

  private JsonNode get(RestClient client, String uri) {
    return client.get().uri(uri).retrieve().body(JsonNode.class);
  }

  public AdminActionResponse execute(AdminActionRequest request) {
    String payload = request.payload() == null ? null : request.payload().toString();
    AdminAction action = repository.save(new AdminAction(
        request.actorId(), request.actionType(), request.targetId(), request.reason(), payload));
    try {
      ResponseEntity<String> response = dispatch(request);
      action.succeed(response.getStatusCode().value(), response.getBody());
    } catch (RestClientResponseException ex) {
      action.fail(ex.getStatusCode().value(), ex.getResponseBodyAsString());
    } catch (Exception ex) {
      action.fail(null, ex.getMessage());
    }
    return AdminActionResponse.from(repository.save(action));
  }

  private ResponseEntity<String> dispatch(AdminActionRequest request) {
    return switch (request.actionType()) {
      case TENANT_STATUS_CHANGE -> patch(
          tenant, "/api/v1/tenants/" + uuid(request.targetId()) + "/status", requiredPayload(request));
      case CHARGER_STATUS_CHANGE -> patch(
          charger, "/api/v1/chargers/" + uuid(request.targetId()) + "/status", requiredPayload(request));
      case USER_DEACTIVATE -> deleteUser(uuid(request.targetId()));
      case CHARGER_HEARTBEAT -> charger.post()
          .uri("/api/v1/chargers/" + uuid(request.targetId()) + "/heartbeat")
          .retrieve().toEntity(String.class);
    };
  }

  private ResponseEntity<String> deleteUser(UUID userId) {
    ResponseEntity<Void> response = user.delete().uri("/api/v1/users/" + userId)
        .retrieve().toBodilessEntity();
    return ResponseEntity.status(response.getStatusCode()).body(null);
  }

  private ResponseEntity<String> patch(RestClient client, String path, JsonNode payload) {
    return client.patch().uri(path).contentType(MediaType.APPLICATION_JSON).body(payload)
        .retrieve().toEntity(String.class);
  }

  private JsonNode requiredPayload(AdminActionRequest request) {
    if (request.payload() == null || request.payload().isNull())
      throw new IllegalArgumentException("payload is required for status changes");
    return request.payload();
  }

  private UUID uuid(String value) {
    try {
      return UUID.fromString(value);
    } catch (IllegalArgumentException ex) {
      throw new IllegalArgumentException("targetId must be a UUID for this action");
    }
  }

  @Transactional(readOnly = true)
  public AdminActionResponse getAction(UUID id) {
    return AdminActionResponse.from(repository.findById(id).orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Admin action not found")));
  }

  @Transactional(readOnly = true)
  public Page<AdminActionResponse> listActions(UUID actorId, Pageable pageable) {
    return repository.findByActorIdOrderByCreatedAtDesc(actorId, pageable).map(AdminActionResponse::from);
  }
}
