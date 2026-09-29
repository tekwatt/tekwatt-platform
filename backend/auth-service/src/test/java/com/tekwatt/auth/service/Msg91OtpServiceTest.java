package com.tekwatt.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class Msg91OtpServiceTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test void acceptsOnlyMatchingVerifiedEmail() throws Exception {
        assertThat(Msg91OtpService.verifiedEmailMatches(
                json.readTree("{\"type\":\"success\",\"message\":{\"email\":\"Amul@Tekwatt.in\"}}"),
                "amul@tekwatt.in")).isTrue();
        assertThat(Msg91OtpService.verifiedEmailMatches(
                json.readTree("{\"type\":\"success\",\"message\":{\"email\":\"other@tekwatt.in\"}}"),
                "amul@tekwatt.in")).isFalse();
        assertThat(Msg91OtpService.verifiedEmailMatches(
                json.readTree("{\"type\":\"error\",\"email\":\"amul@tekwatt.in\"}"),
                "amul@tekwatt.in")).isFalse();
        assertThat(Msg91OtpService.verifiedEmailMatches(
                json.readTree("{\"type\":\"success\",\"mobile\":\"919999999999\"}"),
                "amul@tekwatt.in")).isFalse();
        assertThat(Msg91OtpService.widgetMatchesIfPresent(
                json.readTree("{\"widgetId\":\"different-widget\"}"), "configured-widget")).isFalse();
    }

    @Test void acceptsOnlyMatchingVerifiedPhone() throws Exception {
        assertThat(Msg91OtpService.verifiedPhoneMatches(
                json.readTree("{\"type\":\"success\",\"message\":{\"mobile\":\"919843170206\"}}"),
                "+91 9843170206")).isTrue();
        assertThat(Msg91OtpService.verifiedPhoneMatches(
                json.readTree("{\"type\":\"success\",\"identifier\":\"919843170207\"}"),
                "+919843170206")).isFalse();
        assertThat(Msg91OtpService.verifiedPhoneMatches(
                json.readTree("{\"type\":\"error\",\"mobile\":\"919843170206\"}"),
                "+919843170206")).isFalse();
        assertThat(Msg91OtpService.verifiedPhoneMatches(
                json.readTree("{\"type\":\"success\",\"email\":\"admin@tekwatt.in\"}"),
                "+919843170206")).isFalse();
    }
}
