package com.tekwatt.admin.service;

import com.tekwatt.admin.dto.PlatformModuleResponse;
import com.tekwatt.admin.entity.ModuleInstallation;
import com.tekwatt.admin.repository.ModuleInstallationRepository;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional
public class ModuleService {
    private record Definition(String id,String name,String description,List<String>capabilities,List<String>dependencies,boolean available){}
    private static final List<Definition> CATALOG=List.of(
      new Definition("smart-charging","Smart Charging","OCPP 2.0.1 remote commands, live telemetry and charging profiles.",List.of("Live Monitoring","Remote Control"),List.of("ocpp","telemetry"),true),
      new Definition("fleet-management","Fleet Management","Fleet organisations, drivers, RFID access and charging assets.",List.of("Partners","RFID Cards","Employees","Franchises"),List.of("organization","user","charger"),true),
      new Definition("ocpi-roaming","OCPI Roaming","CPO and eMSP roaming interoperability.",List.of("OCPI Roaming"),List.of("ocpi"),false),
      new Definition("demand-response","Demand Response","Grid signals and managed energy demand.",List.of("Demand Response"),List.of("energy-management"),false),
      new Definition("loyalty","Loyalty & Rewards","Customer points, tiers and charging rewards.",List.of("Loyalty & Rewards"),List.of("loyalty"),false),
      new Definition("advanced-analytics","Advanced Analytics","Extended utilization, station performance and revenue intelligence.",List.of("Analytics","Station Performance","Daily Report"),List.of("analytics","reporting"),true));
    private final ModuleInstallationRepository repository; private final RestClient client; private final Map<String,String> healthUrls;
    public ModuleService(ModuleInstallationRepository repository,RestClient.Builder builder,
      @Value("${tekwatt.services.ocpp:http://localhost:8094}")String ocpp,@Value("${tekwatt.services.telemetry:http://localhost:8095}")String telemetry,
      @Value("${tekwatt.services.organization:http://localhost:8086}")String organization,@Value("${tekwatt.services.user:http://localhost:8082}")String user,
      @Value("${tekwatt.services.charger:http://localhost:8083}")String charger,@Value("${tekwatt.services.analytics:http://localhost:8098}")String analytics,
      @Value("${tekwatt.services.reporting:http://localhost:8099}")String reporting){this.repository=repository;SimpleClientHttpRequestFactory requests=new SimpleClientHttpRequestFactory();requests.setConnectTimeout(1000);requests.setReadTimeout(1000);client=builder.requestFactory(requests).build();healthUrls=Map.of("ocpp",ocpp,"telemetry",telemetry,"organization",organization,"user",user,"charger",charger,"analytics",analytics,"reporting",reporting);}
    @Transactional(readOnly=true) public List<PlatformModuleResponse> list(UUID tenantId){Map<String,ModuleInstallation>installed=repository.findAllByTenantId(tenantId).stream().collect(Collectors.toMap(ModuleInstallation::getModuleKey,Function.identity()));return CATALOG.stream().map(d->response(d,installed.get(d.id()))).toList();}
    public PlatformModuleResponse install(UUID tenantId,String moduleId){Definition definition=definition(moduleId);if(!definition.available())throw new ResponseStatusException(HttpStatus.CONFLICT,"This module has no deployable backend implementation yet");List<String>down=definition.dependencies().stream().filter(dependency->!healthy(dependency)).toList();if(!down.isEmpty())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Required services are unavailable: "+String.join(", ",down));ModuleInstallation installation=repository.findByTenantIdAndModuleKey(tenantId,moduleId).orElseGet(()->repository.save(new ModuleInstallation(tenantId,moduleId)));return response(definition,installation);}
    public void uninstall(UUID tenantId,String moduleId){definition(moduleId);repository.findByTenantIdAndModuleKey(tenantId,moduleId).ifPresent(repository::delete);}
    private PlatformModuleResponse response(Definition d,ModuleInstallation installation){boolean operational=d.available()&&d.dependencies().stream().allMatch(this::healthy);String status=installation==null?(d.available()?"AVAILABLE":"NOT_IMPLEMENTED"):(operational?"INSTALLED":"DEGRADED");return new PlatformModuleResponse(d.id(),d.name(),d.description(),d.capabilities(),d.dependencies(),d.available(),installation!=null,operational,status,installation==null?null:installation.getInstalledAt());}
    private Definition definition(String id){return CATALOG.stream().filter(d->d.id().equals(id)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Unknown module"));}
    private boolean healthy(String dependency){String base=healthUrls.get(dependency);if(base==null)return false;try{return client.get().uri(base+"/actuator/health").retrieve().toBodilessEntity().getStatusCode().is2xxSuccessful();}catch(RestClientException exception){return false;}}
}
