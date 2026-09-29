package com.tekwatt.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.payment.dto.PaymentResponse;
import com.tekwatt.payment.dto.RazorpayOrderRequest;
import com.tekwatt.payment.dto.RazorpayOrderResponse;
import com.tekwatt.payment.dto.RazorpayVerificationRequest;
import com.tekwatt.payment.entity.Payment;
import com.tekwatt.payment.entity.PaymentStatus;
import com.tekwatt.payment.repository.PaymentRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/** Mobile payment actions resolve customer identity and invoice amount on the server. */
@Service
public class CustomerPaymentService {
    private final PaymentRepository payments;
    private final PaymentService paymentService;
    private final RestClient users;
    private final RestClient invoices;

    public CustomerPaymentService(PaymentRepository payments, PaymentService paymentService, RestClient.Builder builder,
            @Value("${tekwatt.services.user:http://localhost:8082}") String userUrl,
            @Value("${tekwatt.services.invoice:http://localhost:8091}") String invoiceUrl) {
        this.payments = payments;
        this.paymentService = paymentService;
        users = builder.clone().baseUrl(userUrl).build();
        invoices = builder.clone().baseUrl(invoiceUrl).build();
    }

    public List<PaymentResponse> mine(String authorization) {
        Customer customer = customer(authorization);
        return payments.findAllByTenantIdAndUserIdOrderByCreatedAtDesc(customer.tenantId(), customer.userId())
                .stream().map(PaymentResponse::from).toList();
    }

    public RazorpayOrderResponse order(String authorization, UUID invoiceId) {
        Customer customer = customer(authorization);
        JsonNode invoice = invoice(customer, invoiceId);
        String status = invoice.path("status").asText();
        if (!"ISSUED".equals(status) && !"OVERDUE".equals(status))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice is not payable");
        UUID billId;
        BigDecimal amount;
        try {
            billId = UUID.fromString(invoice.path("billId").asText());
            amount = new BigDecimal(invoice.path("totalAmount").asText());
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice has no valid payable amount", error);
        }
        if (amount.signum() <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice has no payable amount");
        String currency = invoice.path("currency").asText();
        if (!"INR".equals(currency)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Unsupported invoice currency");
        String idempotencyKey = "invoice:" + invoiceId;
        payments.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {
            if (!customer.tenantId().equals(existing.getTenantId()) || !customer.userId().equals(existing.getUserId())
                    || !invoiceId.equals(existing.getInvoiceId()) || !billId.equals(existing.getBillId())
                    || amount.compareTo(existing.getAmount()) != 0 || !currency.equals(existing.getCurrency()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Existing checkout does not match this invoice");
            if (existing.getStatus() == PaymentStatus.SUCCEEDED)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This invoice has already been paid");
        });
        return paymentService.createRazorpayOrder(new RazorpayOrderRequest(customer.tenantId(), customer.userId(),
                billId, invoiceId, idempotencyKey, amount, currency,
                "TekWatt invoice " + invoice.path("invoiceNumber").asText()));
    }

    public PaymentResponse verify(String authorization, RazorpayVerificationRequest request) {
        Customer customer = customer(authorization);
        Payment payment = ownedPayment(customer, request.paymentId());
        if (!"RAZORPAY".equalsIgnoreCase(payment.getProvider()) || payment.getInvoiceId() == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This is not your invoice checkout");
        return paymentService.verifyRazorpay(request);
    }

    public JsonNode settle(String authorization, UUID invoiceId) {
        Customer customer = customer(authorization);
        JsonNode invoice = invoice(customer, invoiceId);
        if ("PAID".equals(invoice.path("status").asText())) return invoice;
        UUID billId;
        BigDecimal invoiceAmount;
        try {
            billId = UUID.fromString(invoice.path("billId").asText());
            invoiceAmount = new BigDecimal(invoice.path("totalAmount").asText());
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice amount is unavailable", error);
        }
        boolean verified = payments.findAllByTenantIdAndUserIdAndInvoiceIdAndStatus(customer.tenantId(),
                customer.userId(), invoiceId, PaymentStatus.SUCCEEDED).stream()
                .anyMatch(payment -> billId.equals(payment.getBillId())
                        && invoiceAmount.compareTo(payment.getAmount()) == 0
                        && invoice.path("currency").asText().equals(payment.getCurrency())
                        && "RAZORPAY".equalsIgnoreCase(payment.getProvider())
                        && payment.getProviderReference() != null && !payment.getProviderReference().isBlank());
        if (!verified)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A verified payment is required");
        try {
            JsonNode paid = invoices.post().uri("/api/v1/invoices/{id}/pay", invoiceId).retrieve().body(JsonNode.class);
            if (paid == null || paid.isNull()) throw unavailable(new IllegalStateException("Invoice update was empty"));
            return paid;
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 409)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Invoice status changed; refresh payments", error);
            throw unavailable(error);
        } catch (RestClientException error) { throw unavailable(error); }
    }

    private Customer customer(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ") || authorization.substring(7).isBlank())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again");
        JsonNode profile;
        try {
            profile = users.get().uri("/api/v1/users/me").header(HttpHeaders.AUTHORIZATION, authorization)
                    .retrieve().body(JsonNode.class);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 401 || error.getStatusCode().value() == 403)
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sign in again", error);
            if (error.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Customer account is required", error);
            throw unavailable(error);
        } catch (RestClientException error) { throw unavailable(error); }
        try {
            if (profile == null || !"ACTIVE".equalsIgnoreCase(profile.path("status").asText()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Active customer account is required");
            return new Customer(UUID.fromString(profile.path("tenantId").asText()),
                    UUID.fromString(profile.path("id").asText()));
        } catch (IllegalArgumentException error) { throw unavailable(error); }
    }

    private JsonNode invoice(Customer customer, UUID invoiceId) {
        JsonNode invoice;
        try {
            invoice = invoices.get().uri("/api/v1/invoices/{id}", invoiceId).retrieve().body(JsonNode.class);
        } catch (RestClientResponseException error) {
            if (error.getStatusCode().value() == 404)
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found in your account", error);
            throw unavailable(error);
        } catch (RestClientException error) { throw unavailable(error); }
        if (invoice == null || !customer.userId().toString().equals(invoice.path("userId").asText())
                || !customer.tenantId().toString().equals(invoice.path("tenantId").asText()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invoice not found in your account");
        return invoice;
    }

    private Payment ownedPayment(Customer customer, UUID paymentId) {
        Payment payment = payments.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
        if (!customer.tenantId().equals(payment.getTenantId()) || !customer.userId().equals(payment.getUserId()))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        return payment;
    }

    private ResponseStatusException unavailable(Exception error) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Billing service is unavailable", error);
    }

    private record Customer(UUID tenantId, UUID userId) { }
}
