package com.tekwatt.session.client;

import com.tekwatt.session.entity.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.http.HttpMethod.POST;

class SessionBillingClientTest {
    @Test void createsBillAndIssuedInvoiceThenReusesThemOnRetry() {
        var builder = RestClient.builder().baseUrl("http://test");
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = builder.build();
        var billing = new SessionBillingClient(client, client, client);
        var s = session();
        String bill = "{\"id\":\"bill-1\",\"sessionId\":\""+s.getId()+"\",\"subtotal\":10,\"taxAmount\":1.8,\"totalAmount\":11.8,\"currency\":\"INR\",\"status\":\"PENDING\"}";
        String invoice = "{\"id\":\"invoice-1\",\"billId\":\"bill-1\",\"status\":\"DRAFT\"}";
        server.expect(requestTo("http://test/api/v1/bills?tenantId="+s.getTenantId())).andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://test/api/v1/bills")).andExpect(method(POST))
            .andExpect(jsonPath("$.sessionId").value(s.getId().toString()))
            .andExpect(jsonPath("$.energyKwh").value(1))
            .andRespond(withSuccess(bill,MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://test/api/v1/invoices?tenantId="+s.getTenantId())).andRespond(withSuccess("[]",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://test/api/v1/users/"+s.getUserId())).andRespond(withSuccess("{\"tenantId\":\""+s.getTenantId()+"\",\"fullName\":\"Test Customer\",\"email\":\"test@example.com\"}",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://test/api/v1/invoices")).andExpect(method(POST))
            .andExpect(jsonPath("$.totalAmount").value(11.8)).andRespond(withSuccess(invoice,MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://test/api/v1/invoices/invoice-1/issue")).andExpect(method(POST)).andRespond(withSuccess());
        server.expect(requestTo("http://test/api/v1/bills?tenantId="+s.getTenantId())).andRespond(withSuccess("["+bill+"]",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://test/api/v1/invoices?tenantId="+s.getTenantId())).andRespond(withSuccess("["+invoice.replace("DRAFT","ISSUED")+"]",MediaType.APPLICATION_JSON));
        billing.issue(s); billing.issue(s); server.verify();
    }
    @Test void resumesDraftInvoiceWithoutCreatingAnotherBill() {
        var builder = RestClient.builder().baseUrl("http://test");
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = builder.build(); var s = session();
        server.expect(requestTo("http://test/api/v1/bills?tenantId="+s.getTenantId())).andRespond(withSuccess("[{\"id\":\"b\",\"sessionId\":\""+s.getId()+"\",\"totalAmount\":10,\"status\":\"PENDING\"}]",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://test/api/v1/invoices?tenantId="+s.getTenantId())).andRespond(withSuccess("[{\"id\":\"i\",\"billId\":\"b\",\"status\":\"DRAFT\"}]",MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://test/api/v1/invoices/i/issue")).andExpect(method(POST)).andRespond(withSuccess());
        new SessionBillingClient(client,client,client).issue(s); server.verify();
    }
    @Test void freeSessionDoesNotRequestPayment() {
        var builder = RestClient.builder().baseUrl("http://test");
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = builder.build(); var s = session();
        server.expect(requestTo("http://test/api/v1/bills?tenantId="+s.getTenantId())).andRespond(withSuccess("[{\"id\":\"b\",\"sessionId\":\""+s.getId()+"\",\"totalAmount\":0,\"status\":\"PENDING\"}]",MediaType.APPLICATION_JSON));
        new SessionBillingClient(client,client,client).issue(s); server.verify();
    }
    static ChargingSession session() {
        var s = new ChargingSession(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),"T1",BigDecimal.ZERO,BigDecimal.TEN,BigDecimal.ZERO,BigDecimal.ZERO,new BigDecimal("18"),"INR");
        s.stop(new BigDecimal("1000"),SessionStatus.COMPLETED); return s;
    }
}
