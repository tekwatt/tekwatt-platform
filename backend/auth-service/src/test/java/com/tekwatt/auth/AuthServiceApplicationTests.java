package com.tekwatt.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.tekwatt.auth.entity.AppUser;
import com.tekwatt.auth.repository.AppUserRepository;
import com.tekwatt.auth.repository.SmtpSettingsRepository;
import com.tekwatt.auth.repository.Msg91OtpSettingsRepository;
import com.tekwatt.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.*;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.verify;
import java.util.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties={"spring.mail.host=localhost","tekwatt.auth.reset-from=no-reply@tekwatt.in","management.health.mail.enabled=false","tekwatt.auth.smtp-encryption-key=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
class AuthServiceApplicationTests {
    @Autowired TestRestTemplate http;
    @MockBean JavaMailSender mailSender;
    @Autowired AppUserRepository users;
    @Autowired SmtpSettingsRepository smtpSettings;
    @Autowired Msg91OtpSettingsRepository msg91Settings;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired AuthService authService;

    @Test void contextLoads() { }

    @Test void phoneLoginRequiresAVerifiedPhoneLinkedToAnActiveAccount() {
        String email = "phone-" + UUID.randomUUID() + "@tekwatt.in";
        AppUser user = users.save(new AppUser(email, passwordEncoder.encode("PhonePassword@123"), "DRIVER"));
        String access = http.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", "PhonePassword@123"), JsonNode.class)
                .getBody().path("accessToken").asText();
        HttpHeaders headers = new HttpHeaders(); headers.setBearerAuth(access);
        assertThat(http.exchange("/api/v1/auth/otp/msg91/phone", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class).getBody().path("phone").asText()).isBlank();
        assertThat(http.postForEntity("/api/v1/auth/otp/msg91/phone/login",
                Map.of("phone", "+919843170206", "accessToken", "unverified"), JsonNode.class)
                .getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        user.linkVerifiedPhone("919843170206");
        users.saveAndFlush(user);
        assertThat(http.exchange("/api/v1/auth/otp/msg91/phone", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class).getBody().path("phone").asText())
                .isEqualTo("+919843170206");
        // A directory phone alone is never trusted; the authenticator stores only verified bindings.
        assertThat(users.findByVerifiedPhone("919843170206")).isPresent();
    }

    @Test void anonymousRegistrationCannotActivateAnAdministratorRole() {
        String email = "new-admin-" + UUID.randomUUID() + "@tekwatt.in";
        JsonNode registration = http.postForEntity("/api/v1/auth/register",
                Map.of("email", email, "password", "AdminPassword@123"), JsonNode.class).getBody();
        UUID id = users.findByEmailIgnoreCase(email).orElseThrow().getId();
        assertThat(users.findById(id).orElseThrow().getRole()).isEqualTo("DRIVER");
        HttpHeaders headers = new HttpHeaders(); headers.setBearerAuth(registration.path("accessToken").asText());
        assertThat(http.exchange("/api/v1/auth/admin/activate?tenantId=" + UUID.randomUUID(), HttpMethod.POST,
                new HttpEntity<>(Map.of("email", email, "authUserId", id), headers), Void.class)
                .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(users.findById(id).orElseThrow().getRole()).isEqualTo("DRIVER");
        authService.activateAdministrator(email, id);
        assertThat(users.findById(id).orElseThrow().getRole()).isEqualTo("ADMIN");
        assertThat(http.exchange("/api/v1/auth/sessions", HttpMethod.GET,
                new HttpEntity<>(headers), JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test void msg91LoginFailsClosedUntilProviderIsConfigured() {
        assertThat(http.getForEntity("/api/v1/auth/otp/msg91/config", JsonNode.class).getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(http.postForEntity("/api/v1/auth/otp/msg91/login",
                Map.of("email", "admin@tekwatt.in", "accessToken", "unverified-widget-result"), JsonNode.class)
                .getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test void adminCanSaveOtpSettingsWithoutExposingServerAuthkey() {
        String adminEmail = "otp-admin-" + UUID.randomUUID() + "@tekwatt.in";
        users.save(new AppUser(adminEmail, passwordEncoder.encode("AdminPassword@123"), "ADMIN"));
        String adminToken = http.postForEntity("/api/v1/auth/login",
                Map.of("email", adminEmail, "password", "AdminPassword@123"), JsonNode.class)
                .getBody().path("accessToken").asText();
        HttpHeaders adminHeaders = new HttpHeaders(); adminHeaders.setBearerAuth(adminToken);
        String driverEmail = "otp-driver-" + UUID.randomUUID() + "@tekwatt.in";
        String driverToken = http.postForEntity("/api/v1/auth/register",
                Map.of("email", driverEmail, "password", "DriverPassword@123"), JsonNode.class)
                .getBody().path("accessToken").asText();
        HttpHeaders driverHeaders = new HttpHeaders(); driverHeaders.setBearerAuth(driverToken);
        String uri = "/api/v1/auth/otp/msg91/settings?tenantId=" + UUID.randomUUID();
        Map<String, String> credentials = Map.of("widgetId", "test-widget-1234",
                "tokenAuth", "test-widget-token-long-enough", "serverAuthKey", "server-auth-key-long-enough");
        try {
            assertThat(http.exchange(uri, HttpMethod.PUT, new HttpEntity<>(credentials, driverHeaders), JsonNode.class)
                    .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            ResponseEntity<JsonNode> saved = http.exchange(uri, HttpMethod.PUT,
                    new HttpEntity<>(credentials, adminHeaders), JsonNode.class);
            assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(saved.getBody().path("configured").asBoolean()).isTrue();
            assertThat(saved.getBody().toString()).doesNotContain("server-auth-key-long-enough")
                    .doesNotContain("test-widget-token-long-enough");
            var row = msg91Settings.findById(1).orElseThrow();
            assertThat(row.getEncryptedServerAuthKey()).doesNotContain("server-auth-key-long-enough");
            assertThat(row.getEncryptedWidgetToken()).doesNotContain("test-widget-token-long-enough");
            assertThat(http.exchange(uri, HttpMethod.PUT, new HttpEntity<>(Map.of(
                    "widgetId", "test-widget-1234", "tokenAuth", "", "serverAuthKey", ""), adminHeaders),
                    JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.OK);
            ResponseEntity<JsonNode> config = http.getForEntity("/api/v1/auth/otp/msg91/config", JsonNode.class);
            assertThat(config.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(config.getBody().path("tokenAuth").asText()).isEqualTo("test-widget-token-long-enough");
            assertThat(config.getBody().toString()).doesNotContain("server-auth-key-long-enough");
            assertThat(http.exchange(uri, HttpMethod.GET, new HttpEntity<>(driverHeaders), JsonNode.class)
                    .getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        } finally { msg91Settings.deleteAll(); }
    }

    @Test
    void exposesOpenApiWithBearerSecurity() {
        ResponseEntity<JsonNode> response = http.getForEntity("/v3/api-docs", JsonNode.class);
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        JsonNode document = response.getBody();
        assertThat(document).isNotNull();
        assertThat(document.path("info").path("title").asText()).isEqualTo("Auth Service API");
        assertThat(document.path("paths").has("/api/v1/auth/login")).isTrue();
        assertThat(document.path("paths").has("/api/v1/auth/smtp-settings")).isTrue();
        assertThat(document.path("components").path("securitySchemes").path("bearerAuth")
                .path("scheme").asText()).isEqualTo("bearer");
    }

    @Test void listsCurrentSessionAndRevokesItOnLogout(){
        String email="sessions-"+UUID.randomUUID()+"@tekwatt.in";
        ResponseEntity<JsonNode> registered=http.postForEntity("/api/v1/auth/register",Map.of("email",email,"password","Tekwatt@12345"),JsonNode.class);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String access=registered.getBody().path("accessToken").asText(),refresh=registered.getBody().path("refreshToken").asText();
        HttpHeaders headers=new HttpHeaders();headers.setBearerAuth(access);
        ResponseEntity<JsonNode> sessions=http.exchange("/api/v1/auth/sessions",HttpMethod.GET,new HttpEntity<>(headers),JsonNode.class);
        assertThat(sessions.getStatusCode()).isEqualTo(HttpStatus.OK);assertThat(sessions.getBody().isArray()).isTrue();assertThat(sessions.getBody().size()).isEqualTo(1);assertThat(sessions.getBody().get(0).path("current").asBoolean()).isTrue();
        assertThat(http.postForEntity("/api/v1/auth/logout",Map.of("refreshToken",refresh),Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(http.exchange("/api/v1/auth/sessions",HttpMethod.GET,new HttpEntity<>(headers),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void resetsPasswordWithSingleUseEmailCodeAndRevokesSessions() {
        String email="reset-"+UUID.randomUUID()+"@tekwatt.in";
        ResponseEntity<JsonNode> registered=http.postForEntity("/api/v1/auth/register",Map.of("email",email,"password","OldPassword@123"),JsonNode.class);
        assertThat(registered.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String access=registered.getBody().path("accessToken").asText();
        assertThat(http.postForEntity("/api/v1/auth/password-reset/request",Map.of("email",email),Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ArgumentCaptor<SimpleMailMessage> sent=ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        String body=sent.getValue().getText();
        assertThat(body).isNotNull();
        var matcher=java.util.regex.Pattern.compile("\\b[0-9]{8}\\b").matcher(body);
        assertThat(matcher.find()).isTrue();
        String code=matcher.group();
        assertThat(http.postForEntity("/api/v1/auth/password-reset/confirm",Map.of("email",email,"code","00000000","newPassword","NewPassword@123"),Void.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.postForEntity("/api/v1/auth/password-reset/confirm",Map.of("email",email,"code",code,"newPassword","NewPassword@123"),Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(http.postForEntity("/api/v1/auth/password-reset/confirm",Map.of("email",email,"code",code,"newPassword","OtherPassword@123"),Void.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(http.postForEntity("/api/v1/auth/login",Map.of("email",email,"password","OldPassword@123"),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(http.postForEntity("/api/v1/auth/login",Map.of("email",email,"password","NewPassword@123"),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders headers=new HttpHeaders();headers.setBearerAuth(access);
        assertThat(http.exchange("/api/v1/auth/sessions",HttpMethod.GET,new HttpEntity<>(headers),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void resetRequestDoesNotRevealAccountsAndFiveWrongCodesLockTheCode() {
        String email="locked-"+UUID.randomUUID()+"@tekwatt.in";
        assertThat(http.postForEntity("/api/v1/auth/password-reset/request",Map.of("email",email),Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(http.postForEntity("/api/v1/auth/register",Map.of("email",email,"password","OldPassword@123"),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(http.postForEntity("/api/v1/auth/password-reset/request",Map.of("email",email),Void.class).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        ArgumentCaptor<SimpleMailMessage> sent=ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(sent.capture());
        var matcher=java.util.regex.Pattern.compile("\\b[0-9]{8}\\b").matcher(sent.getValue().getText());
        assertThat(matcher.find()).isTrue();
        String correctCode=matcher.group();
        String wrongCode=correctCode.equals("00000000")?"99999999":"00000000";
        for(int attempt=0;attempt<5;attempt++) {
            assertThat(http.postForEntity("/api/v1/auth/password-reset/confirm",Map.of("email",email,"code",wrongCode,"newPassword","NewPassword@123"),Void.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
        assertThat(http.postForEntity("/api/v1/auth/password-reset/confirm",Map.of("email",email,"code",correctCode,"newPassword","NewPassword@123"),Void.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void onlyAdministratorCanStoreEncryptedSmtpCredentials() {
        String email="smtp-admin-"+UUID.randomUUID()+"@tekwatt.in";
        users.save(new AppUser(email,passwordEncoder.encode("AdminPassword@123"),"ADMIN"));
        ResponseEntity<JsonNode> login=http.postForEntity("/api/v1/auth/login",Map.of("email",email,"password","AdminPassword@123"),JsonNode.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        HttpHeaders headers=new HttpHeaders();headers.setBearerAuth(login.getBody().path("accessToken").asText());
        UUID tenantId=UUID.randomUUID();
        String uri="/api/v1/auth/smtp-settings?tenantId="+tenantId;
        Map<String,Object> body=Map.of("host","smtp.example.com","port",587,"securityMode","STARTTLS",
                "username","mailer@example.com","password","super-secret-app-password",
                "fromEmail","no-reply@example.com","replyTo","help@example.com");
        String customerEmail="smtp-customer-"+UUID.randomUUID()+"@tekwatt.in";
        ResponseEntity<JsonNode> customer=http.postForEntity("/api/v1/auth/register",Map.of("email",customerEmail,"password","CustomerPass@123"),JsonNode.class);
        HttpHeaders customerHeaders=new HttpHeaders();customerHeaders.setBearerAuth(customer.getBody().path("accessToken").asText());
        assertThat(http.exchange(uri,HttpMethod.PUT,new HttpEntity<>(body,customerHeaders),JsonNode.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        try {
            ResponseEntity<JsonNode> saved=http.exchange(uri,HttpMethod.PUT,new HttpEntity<>(body,headers),JsonNode.class);
            assertThat(saved.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(saved.getBody().path("passwordConfigured").asBoolean()).isTrue();
            assertThat(saved.getBody().toString()).doesNotContain("super-secret-app-password");
            assertThat(smtpSettings.findById(1).orElseThrow().getEncryptedPassword()).doesNotContain("super-secret-app-password");
            assertThat(http.exchange(uri,HttpMethod.GET,new HttpEntity<>(headers),JsonNode.class).getBody().path("host").asText()).isEqualTo("smtp.example.com");
        } finally { smtpSettings.deleteAll(); }
    }
}
