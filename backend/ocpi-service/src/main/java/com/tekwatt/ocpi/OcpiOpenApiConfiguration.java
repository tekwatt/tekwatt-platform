package com.tekwatt.ocpi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcpiOpenApiConfiguration {
    @Bean
    OpenAPI ocpiOpenApi(@Value("${tekwatt.ocpi.public-base-url}") String publicBaseUrl) {
        return new OpenAPI()
                .addServersItem(new Server().url(publicBaseUrl.replaceAll("/+$", "")))
                .info(new Info().title("TekWatt OCPI API").version("2.2.1")
                        .description("CPO Locations Sender (read-only). The partner token must be sent as "
                                + "Authorization: Token <base64-encoded token>. Credentials exchange and other "
                                + "OCPI modules are not implemented."))
                .components(new Components().addSecuritySchemes("ocpiToken",
                        new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("Authorization")
                                .description("Enter the complete value: Token <base64-encoded partner token>")))
                .addSecurityItem(new SecurityRequirement().addList("ocpiToken"));
    }
}
