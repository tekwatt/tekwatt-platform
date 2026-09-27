package com.tekwatt.notification.service;

import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SmsProviderRouter {
    private final Msg91SmsClient msg91;
    private final TwilioSmsClient twilio;
    private final SmsProviderCredentialService credentials;
    private final RestClient settings;
    public SmsProviderRouter(Msg91SmsClient msg91,TwilioSmsClient twilio,SmsProviderCredentialService credentials,RestClient.Builder builder,
            @Value("${tekwatt.services.admin:http://localhost:8100}") String adminUrl){
        this.msg91=msg91;this.twilio=twilio;this.credentials=credentials;
        this.settings=builder.baseUrl(adminUrl).build();
    }
    public String send(UUID tenantId,String recipient,String body){
        return send(tenantId, recipient, body, null);
    }
    public String send(UUID tenantId,String recipient,String body,String templateKey){
        Map<?,?> configured;
        try{
            configured=settings.get().uri(uri->uri.path("/api/v1/admin/governance/settings")
                    .queryParam("tenantId",tenantId).build()).retrieve().body(Map.class);
        }catch(Exception error){throw new Msg91SmsClient.SmsDeliveryException("SMS provider settings are temporarily unavailable.");}
        Object selected=configured==null?null:configured.get("smsProvider");
        String provider=selected==null?"MSG91":String.valueOf(selected);
        var saved=credentials.credentials(tenantId,provider);
        return switch(provider){
            case "MSG91" -> saved.map(value->msg91.send(recipient,body,value.secret(),
                    templateFor(value, templateKey),value.messageVariable()))
                    .orElseGet(()->{
                        if (isChargingLifecycle(templateKey))
                            throw new Msg91SmsClient.SmsDeliveryException("An approved MSG91 charging template is not configured for this workspace.");
                        return msg91.send(recipient,body);
                    });
            case "TWILIO" -> saved.map(value->twilio.send(recipient,body,value.publicIdentifier(),value.secret(),value.sender()))
                    .orElseGet(()->twilio.send(recipient,body));
            default -> throw new Msg91SmsClient.SmsDeliveryException("Unsupported SMS provider configured for this workspace.");
        };
    }
    private boolean isChargingLifecycle(String templateKey) {
        return "charging-started".equals(templateKey) || "charging-completed".equals(templateKey);
    }
    private String templateFor(SmsProviderCredentialService.Credentials value, String templateKey) {
        if (isChargingLifecycle(templateKey)) {
            String template = "charging-started".equals(templateKey)
                    ? value.chargingStartedTemplateId() : value.chargingCompletedTemplateId();
            if (template == null || template.isBlank())
                throw new Msg91SmsClient.SmsDeliveryException("An approved MSG91 template for this charging event is not configured.");
            return template;
        }
        return value.templateId();
    }
}
