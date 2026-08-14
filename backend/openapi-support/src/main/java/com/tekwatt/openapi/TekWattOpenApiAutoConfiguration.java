package com.tekwatt.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
public class TekWattOpenApiAutoConfiguration {
  @Bean
  OpenAPI tekWattOpenApi(Environment environment) {
    String serviceName = environment.getProperty("spring.application.name", "tekwatt-service");
    String bearer = "bearerAuth";
    return new OpenAPI()
        .info(new Info().title(displayName(serviceName) + " API").version("v1")
            .description("TekWatt EV Charging Platform service API"))
        .components(new Components().addSecuritySchemes(bearer,
            new SecurityScheme().name(bearer).type(SecurityScheme.Type.HTTP)
                .scheme("bearer").bearerFormat("JWT")))
        .addSecurityItem(new SecurityRequirement().addList(bearer));
  }

  private String displayName(String value) {
    String[] words = value.replace('-', ' ').split("\\s+");
    StringBuilder result = new StringBuilder();
    for (String word : words) {
      if (!word.isBlank()) {
        if (!result.isEmpty()) result.append(' ');
        result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
      }
    }
    return result.toString();
  }
}
