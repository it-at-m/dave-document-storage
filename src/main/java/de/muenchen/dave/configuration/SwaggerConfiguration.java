/*
 * Copyright (c): it@M - Dienstleister für Informations- und Telekommunikationstechnik
 * der Landeshauptstadt München, 2023
 */
package de.muenchen.dave.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfiguration {

    @Bean
    public OpenAPI openAPI(@Value("${info.application.version}") final String buildVersion) {
        return new OpenAPI()
                .info(
                        new Info()
                                .title("DAVe Dokumentenspeicher")
                                .version(buildVersion)
                                .description("Integrationsbaustein zum Abfragen des Dokumentenspeichers"));
    }

    @Bean
    public String[] whitelist() {
        return new String[] {
                // -- swagger ui
                "/v3/api-docs/**",
                "/swagger-resources/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
        };
    }
}
