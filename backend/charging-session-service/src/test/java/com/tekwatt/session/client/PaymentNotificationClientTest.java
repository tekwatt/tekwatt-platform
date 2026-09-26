package com.tekwatt.session.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.junit.jupiter.api.Assertions.*;

class PaymentNotificationClientTest {
    @Test void queuesAndSendsInvoiceLinkWithoutRepeatingAcceptedSms() throws Exception {
        var builder=RestClient.builder().baseUrl("http://test"); var server=MockRestServiceServer.bindTo(builder).build();
        var client=new PaymentNotificationClient(builder.build(),true,"https://portal.example.com");
        var s=SessionBillingClientTest.session(); var json=new ObjectMapper();
        var i=json.readTree("{\"id\":\"11111111-1111-4111-8111-111111111111\",\"invoiceNumber\":\"INV-1\",\"status\":\"ISSUED\",\"totalAmount\":100,\"currency\":\"INR\",\"taxAmount\":18,\"dueDate\":\"2026-09-26\"}");
        var u=json.readTree("{\"tenantId\":\""+s.getTenantId()+"\",\"phone\":\"+919876543210\"}");
        server.expect(requestTo("http://test/api/v1/notifications"))
            .andExpect(jsonPath("$.idempotencyKey").value("pay-sms:11111111-1111-4111-8111-111111111111"))
            .andExpect(jsonPath("$.body").value(org.hamcrest.Matchers.containsString("https://portal.example.com/?invoice=")))
            .andRespond(withSuccess("{\"id\":\"n1\",\"status\":\"QUEUED\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://test/api/v1/notifications/n1/send")).andRespond(withSuccess("{\"status\":\"SENT\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://test/api/v1/notifications")).andRespond(withSuccess("{\"id\":\"n1\",\"status\":\"SENT\"}",MediaType.APPLICATION_JSON));
        client.send(s,i,u); client.send(s,i,u); server.verify();
    }
    @Test void missingPhoneDoesNotPretendToSend() throws Exception {
        var s=SessionBillingClientTest.session(); var json=new ObjectMapper();
        var client=new PaymentNotificationClient(RestClient.create(),true,"https://portal.example.com");
        assertThrows(IllegalStateException.class,()->client.send(s,json.readTree("{\"status\":\"ISSUED\"}"),json.readTree("{\"tenantId\":\""+s.getTenantId()+"\"}")));
    }
}
