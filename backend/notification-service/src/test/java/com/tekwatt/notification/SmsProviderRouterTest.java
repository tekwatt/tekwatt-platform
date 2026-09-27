package com.tekwatt.notification;

import com.tekwatt.notification.service.Msg91SmsClient;
import com.tekwatt.notification.service.SmsProviderRouter;
import com.tekwatt.notification.service.SmsProviderCredentialService;
import com.tekwatt.notification.service.TwilioSmsClient;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class SmsProviderRouterTest {
    @Test void usesSavedWorkspaceCredentialsInsteadOfEnvironmentFallback() {
        UUID tenant=UUID.randomUUID();
        var msg91=mock(Msg91SmsClient.class);var twilio=mock(TwilioSmsClient.class);
        var credentials=mock(SmsProviderCredentialService.class);
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://admin/api/v1/admin/governance/settings?tenantId="+tenant))
                .andRespond(withSuccess("{\"smsProvider\":\"MSG91\"}",MediaType.APPLICATION_JSON));
        when(credentials.credentials(tenant,"MSG91")).thenReturn(Optional.of(
                new SmsProviderCredentialService.Credentials("","","flow-1","start-flow","complete-flow","message","saved-key")));
        when(msg91.send("+919876543210","Payment due","saved-key","flow-1","message")).thenReturn("REQUEST-1");
        var router=new SmsProviderRouter(msg91,twilio,credentials,builder,"http://admin");
        assertThat(router.send(tenant,"+919876543210","Payment due")).isEqualTo("REQUEST-1");
        verify(msg91,never()).send("+919876543210","Payment due");
        server.verify();
    }
    @Test void sendsThroughTheWorkspaceSelectedProvider() {
        UUID tenant=UUID.randomUUID();
        var msg91=mock(Msg91SmsClient.class);var twilio=mock(TwilioSmsClient.class);
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://admin/api/v1/admin/governance/settings?tenantId="+tenant))
                .andRespond(withSuccess("{\"smsProvider\":\"TWILIO\"}",MediaType.APPLICATION_JSON));
        when(twilio.send("+919876543210","Payment due")).thenReturn("SM123");
        var credentials=mock(SmsProviderCredentialService.class);
        when(credentials.credentials(tenant,"TWILIO")).thenReturn(Optional.empty());
        var router=new SmsProviderRouter(msg91,twilio,credentials,builder,"http://admin");
        assertThat(router.send(tenant,"+919876543210","Payment due")).isEqualTo("SM123");
        verifyNoInteractions(msg91);server.verify();
    }
    @Test void usesChargingStartTemplateWithoutFallingBackToGeneralTemplate() {
        UUID tenant=UUID.randomUUID();
        var msg91=mock(Msg91SmsClient.class);var twilio=mock(TwilioSmsClient.class);
        var credentials=mock(SmsProviderCredentialService.class);
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://admin/api/v1/admin/governance/settings?tenantId="+tenant))
                .andRespond(withSuccess("{\"smsProvider\":\"MSG91\"}",MediaType.APPLICATION_JSON));
        when(credentials.credentials(tenant,"MSG91")).thenReturn(Optional.of(
                new SmsProviderCredentialService.Credentials("","","general-flow","start-flow","complete-flow","message","saved-key")));
        when(msg91.send("+919876543210","Started","saved-key","start-flow","message")).thenReturn("REQUEST-2");
        var router=new SmsProviderRouter(msg91,twilio,credentials,builder,"http://admin");
        assertThat(router.send(tenant,"+919876543210","Started","charging-started")).isEqualTo("REQUEST-2");
        verify(msg91,never()).send("+919876543210","Started","saved-key","general-flow","message");
        server.verify();
    }
    @Test void refusesChargingSmsWhenEventTemplateIsMissing() {
        UUID tenant=UUID.randomUUID();
        var msg91=mock(Msg91SmsClient.class);var twilio=mock(TwilioSmsClient.class);
        var credentials=mock(SmsProviderCredentialService.class);
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://admin/api/v1/admin/governance/settings?tenantId="+tenant))
                .andRespond(withSuccess("{\"smsProvider\":\"MSG91\"}",MediaType.APPLICATION_JSON));
        when(credentials.credentials(tenant,"MSG91")).thenReturn(Optional.of(
                new SmsProviderCredentialService.Credentials("","","general-flow","start-flow","","message","saved-key")));
        var router=new SmsProviderRouter(msg91,twilio,credentials,builder,"http://admin");
        assertThatThrownBy(()->router.send(tenant,"+919876543210","Completed","charging-completed"))
                .isInstanceOf(Msg91SmsClient.SmsDeliveryException.class);
        verifyNoInteractions(msg91);
        server.verify();
    }
    @Test void unknownProviderNeverFallsBackToAnotherSender() {
        UUID tenant=UUID.randomUUID();
        var msg91=mock(Msg91SmsClient.class);var twilio=mock(TwilioSmsClient.class);
        var builder=RestClient.builder();var server=MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://admin/api/v1/admin/governance/settings?tenantId="+tenant))
                .andRespond(withSuccess("{\"smsProvider\":\"UNKNOWN\"}",MediaType.APPLICATION_JSON));
        var credentials=mock(SmsProviderCredentialService.class);
        when(credentials.credentials(tenant,"UNKNOWN")).thenReturn(Optional.empty());
        var router=new SmsProviderRouter(msg91,twilio,credentials,builder,"http://admin");
        assertThatThrownBy(()->router.send(tenant,"+919876543210","Payment due"))
                .isInstanceOf(Msg91SmsClient.SmsDeliveryException.class);
        verifyNoInteractions(msg91,twilio);server.verify();
    }
}
