/*
 * Copyright (c): it@M - Dienstleister für Informations- und Telekommunikationstechnik
 * der Landeshauptstadt München, 2023
 */
package de.muenchen.dave.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class JacksonConfiguration {

    private final ObjectMapper objectMapper;

    @PostConstruct
    public void objectMapper() {
        this.objectMapper.enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
    }
}
