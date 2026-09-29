package com.tekwatt.payment.service;

import com.tekwatt.payment.dto.RazorpayOrderRequest;
import com.tekwatt.payment.dto.RazorpayOrderResponse;
import com.tekwatt.payment.dto.RazorpayVerificationRequest;
import com.tekwatt.payment.entity.Payment;
import com.tekwatt.payment.entity.PaymentStatus;
import com.tekwatt.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CustomerPaymentServiceTest {
    @Test void orderDerivesAmountAndCustomerFromVerifiedServerRecords() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID(), invoice = UUID.randomUUID(), bill = UUID.randomUUID();
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentService payments = mock(PaymentService.class);
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess(profile(tenant, user), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://invoices/api/v1/invoices/" + invoice))
                .andRespond(withSuccess(invoice(tenant, user, bill, "123.45"), MediaType.APPLICATION_JSON));
        when(payments.createRazorpayOrder(any())).thenReturn(new RazorpayOrderResponse(UUID.randomUUID(),
                "order_1", "key_1", 12345, "INR", "Invoice"));
        var service = new CustomerPaymentService(repository, payments, builder, "http://users", "http://invoices");
        service.order("Bearer customer", invoice);
        var request = ArgumentCaptor.forClass(RazorpayOrderRequest.class);
        verify(payments).createRazorpayOrder(request.capture());
        assertThat(request.getValue().tenantId()).isEqualTo(tenant);
        assertThat(request.getValue().userId()).isEqualTo(user);
        assertThat(request.getValue().billId()).isEqualTo(bill);
        assertThat(request.getValue().invoiceId()).isEqualTo(invoice);
        assertThat(request.getValue().amount()).isEqualByComparingTo(new BigDecimal("123.45"));
        server.verify();
    }

    @Test void cannotCreateOrderForAnotherCustomer() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID(), invoice = UUID.randomUUID();
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentService payments = mock(PaymentService.class);
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess(profile(tenant, user), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://invoices/api/v1/invoices/" + invoice))
                .andRespond(withSuccess(invoice(tenant, UUID.randomUUID(), UUID.randomUUID(), "10.00"), MediaType.APPLICATION_JSON));
        var service = new CustomerPaymentService(repository, payments, builder, "http://users", "http://invoices");
        assertThatThrownBy(() -> service.order("Bearer customer", invoice))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verifyNoInteractions(payments);
        server.verify();
    }

    @Test void cannotVerifyAnotherCustomersPayment() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID(), paymentId = UUID.randomUUID();
        PaymentRepository repository = mock(PaymentRepository.class);
        PaymentService payments = mock(PaymentService.class);
        when(repository.findById(paymentId)).thenReturn(Optional.of(new Payment(tenant, UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "other", "RAZORPAY", "ONLINE", BigDecimal.TEN, "INR")));
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess(profile(tenant, user), MediaType.APPLICATION_JSON));
        var service = new CustomerPaymentService(repository, payments, builder, "http://users", "http://invoices");
        assertThatThrownBy(() -> service.verify("Bearer customer",
                new RazorpayVerificationRequest(paymentId, "pay_1", "order_1", "signature")))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("404");
        verifyNoInteractions(payments);
        server.verify();
    }

    @Test void cannotSettleWithoutVerifiedPayment() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID(), invoice = UUID.randomUUID();
        PaymentRepository repository = mock(PaymentRepository.class);
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess(profile(tenant, user), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://invoices/api/v1/invoices/" + invoice))
                .andRespond(withSuccess(invoice(tenant, user, UUID.randomUUID(), "10.00"), MediaType.APPLICATION_JSON));
        var service = new CustomerPaymentService(repository, mock(PaymentService.class), builder, "http://users", "http://invoices");
        assertThatThrownBy(() -> service.settle("Bearer customer", invoice))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        server.verify();
    }

    @Test void cannotSettleAnInvoiceWithAnUnderpaidTransaction() {
        UUID tenant = UUID.randomUUID(), user = UUID.randomUUID(), invoice = UUID.randomUUID(), bill = UUID.randomUUID();
        PaymentRepository repository = mock(PaymentRepository.class);
        Payment underpaid = new Payment(tenant, user, bill, invoice, "invoice:" + invoice,
                "RAZORPAY", "ONLINE", new BigDecimal("1.00"), "INR");
        underpaid.succeed("pay_1");
        when(repository.findAllByTenantIdAndUserIdAndInvoiceIdAndStatus(tenant, user, invoice,
                PaymentStatus.SUCCEEDED)).thenReturn(List.of(underpaid));
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://users/api/v1/users/me"))
                .andRespond(withSuccess(profile(tenant, user), MediaType.APPLICATION_JSON));
        server.expect(requestTo("http://invoices/api/v1/invoices/" + invoice))
                .andRespond(withSuccess(invoice(tenant, user, bill, "100.00"), MediaType.APPLICATION_JSON));
        var service = new CustomerPaymentService(repository, mock(PaymentService.class), builder, "http://users", "http://invoices");
        assertThatThrownBy(() -> service.settle("Bearer customer", invoice))
                .isInstanceOf(ResponseStatusException.class).hasMessageContaining("409");
        server.verify();
    }

    private static String profile(UUID tenant, UUID user) {
        return "{\"id\":\"" + user + "\",\"tenantId\":\"" + tenant + "\",\"status\":\"ACTIVE\"}";
    }

    private static String invoice(UUID tenant, UUID user, UUID bill, String amount) {
        return "{\"tenantId\":\"" + tenant + "\",\"userId\":\"" + user + "\",\"billId\":\"" + bill
                + "\",\"status\":\"ISSUED\",\"totalAmount\":\"" + amount
                + "\",\"currency\":\"INR\",\"invoiceNumber\":\"INV-1\"}";
    }
}
