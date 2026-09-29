package com.tekwatt.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tekwatt.auth.dto.Msg91OtpSettingsRequest;
import com.tekwatt.auth.entity.Msg91OtpSettings;
import com.tekwatt.auth.entity.UsedOtpToken;
import com.tekwatt.auth.repository.Msg91OtpSettingsRepository;
import com.tekwatt.auth.repository.UsedOtpTokenRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Msg91OtpService {
    private static final URI VERIFY_URL = URI.create("https://control.msg91.com/api/v5/widget/verifyAccessToken");
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json;
    private final UsedOtpTokenRepository usedTokens;
    private final Msg91OtpSettingsRepository settings;
    private final String encryptionKey;
    private final SecureRandom random = new SecureRandom();
    private final String widgetId;
    private final String widgetToken;
    private final String serverAuthKey;

    public Msg91OtpService(ObjectMapper json, UsedOtpTokenRepository usedTokens,
            Msg91OtpSettingsRepository settings,
            @Value("${tekwatt.auth.smtp-encryption-key:}") String encryptionKey,
            @Value("${tekwatt.auth.msg91.widget-id:}") String widgetId,
            @Value("${tekwatt.auth.msg91.widget-token:}") String widgetToken,
            @Value("${tekwatt.auth.msg91.server-auth-key:}") String serverAuthKey) {
        this.json = json; this.usedTokens = usedTokens; this.settings = settings;
        this.encryptionKey = encryptionKey; this.widgetId = widgetId; this.widgetToken = widgetToken;
        this.serverAuthKey = serverAuthKey;
    }

    public WidgetConfig widgetConfig() {
        Configuration active = active();
        // This restricted widget token is intended for the browser. The account Authkey never leaves the server.
        return new WidgetConfig(active.widgetId(), active.widgetToken());
    }

    @Transactional(readOnly = true)
    public SettingsSummary status() {
        return settings.findById(1).map(row -> new SettingsSummary(true, "DATABASE", row.getWidgetId(),
                true, true, row.getUpdatedAt())).orElseGet(() -> new SettingsSummary(
                !widgetId.isBlank() && !widgetToken.isBlank() && !serverAuthKey.isBlank(),
                widgetId.isBlank() && widgetToken.isBlank() && serverAuthKey.isBlank() ? "NONE" : "ENVIRONMENT",
                widgetId, !widgetToken.isBlank(), !serverAuthKey.isBlank(), null));
    }

    @Transactional
    public SettingsSummary save(Msg91OtpSettingsRequest request) {
        String id = request.widgetId().trim();
        if (!id.matches("[A-Za-z0-9_-]{8,128}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid MSG91 widget ID.");
        Msg91OtpSettings prior = settings.findById(1).orElse(null);
        String token = request.tokenAuth() == null ? "" : request.tokenAuth().trim();
        String authKey = request.serverAuthKey() == null ? "" : request.serverAuthKey().trim();
        if ((token.isBlank() || authKey.isBlank()) && prior == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter both the widget token and server Authkey.");
        if ((!token.isBlank() && (token.length() < 16 || containsWhitespace(token)))
                || (!authKey.isBlank() && (authKey.length() < 16 || containsWhitespace(authKey))))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MSG91 credentials have an invalid format.");
        String encryptedToken = token.isBlank() ? prior.getEncryptedWidgetToken() : encrypt(token, "widget");
        String encryptedKey = authKey.isBlank() ? prior.getEncryptedServerAuthKey() : encrypt(authKey, "server");
        if (prior == null) settings.save(new Msg91OtpSettings(id, encryptedToken, encryptedKey));
        else prior.update(id, encryptedToken, encryptedKey);
        return status();
    }

    @Transactional
    public void verifyEmail(String accessToken, String email) {
        verify(accessToken, response -> verifiedEmailMatches(response, email));
    }

    @Transactional
    public void verifyPhone(String accessToken, String phone) {
        String expected = normalizePhone(phone);
        verify(accessToken, response -> verifiedPhoneMatches(response, expected));
    }

    private void verify(String accessToken, java.util.function.Predicate<JsonNode> identifierMatches) {
        Configuration active = active();
        try {
            String body = json.writeValueAsString(Map.of("authkey", active.serverAuthKey(), "access-token", accessToken));
            HttpRequest request = HttpRequest.newBuilder(VERIFY_URL).timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode verified = response.statusCode() == 200 ? json.readTree(response.body()) : null;
            if (verified == null || !identifierMatches.test(verified)
                    || !widgetMatchesIfPresent(verified, active.widgetId()))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP verification failed");
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(accessToken.getBytes(StandardCharsets.UTF_8)));
            if (usedTokens.existsById(hash))
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP token has already been used");
            usedTokens.saveAndFlush(new UsedOtpToken(hash));
        } catch (ResponseStatusException failure) {
            throw failure;
        } catch (DataIntegrityViolationException failure) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "OTP token has already been used");
        } catch (Exception failure) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "OTP provider verification is unavailable");
        }
    }

    static boolean verifiedEmailMatches(JsonNode response, String email) {
        if (!successful(response)) return false;
        String expected = email.trim().toLowerCase(java.util.Locale.ROOT);
        if (response.path("message").isTextual()
                && response.path("message").asText().trim().toLowerCase(java.util.Locale.ROOT).equals(expected))
            return true;
        for (JsonNode node : new JsonNode[]{response, response.path("data"), response.path("message")}) {
            for (String field : new String[]{"email", "identifier"}) {
                String actual = node.path(field).asText("").trim().toLowerCase(java.util.Locale.ROOT);
                if (actual.equals(expected) && actual.contains("@")) return true;
            }
        }
        return false;
    }

    static boolean verifiedPhoneMatches(JsonNode response, String phone) {
        if (!successful(response)) return false;
        String expected = normalizePhone(phone);
        for (JsonNode node : new JsonNode[]{response, response.path("data"), response.path("message")}) {
            if (node.isTextual() && matchesPhone(node.asText(), expected)) return true;
            for (String field : new String[]{"mobile", "phone", "phoneNumber", "identifier"}) {
                if (matchesPhone(node.path(field).asText(""), expected)) return true;
            }
        }
        return false;
    }

    private static boolean successful(JsonNode response) {
        return "success".equalsIgnoreCase(response.path("type").asText())
                || "success".equalsIgnoreCase(response.path("status").asText())
                || response.path("success").asBoolean(false);
    }

    private static boolean matchesPhone(String value, String expected) {
        try { return normalizePhone(value).equals(expected); }
        catch (ResponseStatusException ignored) { return false; }
    }

    public static String normalizePhone(String phone) {
        String digits = phone == null ? "" : phone.trim().replaceAll("[+\\s()-]", "");
        if (!digits.matches("[1-9][0-9]{9,14}"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a phone number with country code.");
        return digits;
    }

    static boolean widgetMatchesIfPresent(JsonNode response, String expected) {
        for (JsonNode node : new JsonNode[]{response, response.path("data"), response.path("message")}) {
            for (String key : new String[]{"widgetId", "widget_id"}) {
                String actual = node.path(key).asText("");
                if (!actual.isBlank() && !actual.equals(expected)) return false;
            }
        }
        return true;
    }

    private Configuration active() {
        Msg91OtpSettings row = settings.findById(1).orElse(null);
        if (row != null) return new Configuration(row.getWidgetId(),
                decrypt(row.getEncryptedWidgetToken(), "widget"), decrypt(row.getEncryptedServerAuthKey(), "server"));
        if (widgetId.isBlank() || widgetToken.isBlank() || serverAuthKey.isBlank())
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "OTP sign-in is not configured");
        return new Configuration(widgetId, widgetToken, serverAuthKey);
    }

    private boolean containsWhitespace(String value) { return value.chars().anyMatch(Character::isWhitespace); }

    private String encrypt(String plain, String kind) {
        try {
            byte[] iv = new byte[12]; random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            cipher.updateAAD(("tekwatt.msg91.otp.v1." + kind).getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] result = Arrays.copyOf(iv, iv.length + ciphertext.length);
            System.arraycopy(ciphertext, 0, result, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(result);
        } catch (ResponseStatusException failure) { throw failure; }
        catch (Exception failure) { throw new IllegalStateException("OTP credential could not be stored."); }
    }

    private String decrypt(String stored, String kind) {
        try {
            byte[] input = Base64.getDecoder().decode(stored);
            if (input.length < 29) throw new IllegalArgumentException("Invalid encrypted value");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, input, 0, 12));
            cipher.updateAAD(("tekwatt.msg91.otp.v1." + kind).getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(input, 12, input.length - 12), StandardCharsets.UTF_8);
        } catch (ResponseStatusException failure) { throw failure; }
        catch (Exception failure) { throw new IllegalStateException("Stored OTP credential could not be read."); }
    }

    private SecretKeySpec key() {
        try {
            byte[] decoded = Base64.getDecoder().decode(encryptionKey);
            if (decoded.length != 32) throw new IllegalArgumentException("Expected a 256-bit key");
            return new SecretKeySpec(decoded, "AES");
        } catch (IllegalArgumentException failure) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "OTP credential storage needs a server encryption key. Ask the platform administrator to configure it.");
        }
    }

    private record Configuration(String widgetId, String widgetToken, String serverAuthKey) { }
    public record WidgetConfig(String widgetId, String tokenAuth) { }
    public record SettingsSummary(boolean configured, String source, String widgetId,
                                  boolean widgetTokenConfigured, boolean serverAuthKeyConfigured, Instant updatedAt) { }
}
