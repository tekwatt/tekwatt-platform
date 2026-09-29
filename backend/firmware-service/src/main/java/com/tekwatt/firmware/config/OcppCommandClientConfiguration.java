package com.tekwatt.firmware.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcppCommandClientConfiguration {
    @Bean
    RestClientCustomizer internalCommandCredential(
            @Value("${tekwatt.ocpp.internal-command-key:}") String internalCommandKey) {
        return builder -> {
            if (internalCommandKey != null && !internalCommandKey.isBlank())
                builder.defaultHeader("X-Tekwatt-Internal-Command-Key", internalCommandKey);
        };
    }
}
