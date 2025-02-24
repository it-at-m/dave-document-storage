/*
 * Copyright (c): it@M - Dienstleister für Informations- und Telekommunikationstechnik
 * der Landeshauptstadt München, 2023
 */
package de.muenchen.dave.configuration;

import java.util.Arrays;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * The central class for configuration of all security aspects.
 */
@Configuration
@Profile("!no-security")
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final String[] whitelist;

    /**
     * Absichern der Rest-Endpunkte mit Definition der Ausnahmen.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(request -> request
                        .requestMatchers(getPathMatchersForPermitAll())
                        .permitAll()
                        .requestMatchers(AntPathRequestMatcher.antMatcher("/**"))
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    private AntPathRequestMatcher[] getPathMatchersForPermitAll() {
        return Stream
                .concat(
                        Stream.of(
                                // allow access to /actuator/info
                                AntPathRequestMatcher.antMatcher("/actuator/info"),
                                // allow access to /actuator/health for OpenShift Health Check
                                AntPathRequestMatcher.antMatcher("/actuator/health"),
                                // allow access to /actuator/health/liveness for OpenShift Liveness Check
                                AntPathRequestMatcher.antMatcher("/actuator/health/liveness"),
                                // allow access to /actuator/health/readiness for OpenShift Readiness Check
                                AntPathRequestMatcher.antMatcher("/actuator/health/readiness"),
                                // allow access to /actuator/metrics for Prometheus monitoring in OpenShift
                                AntPathRequestMatcher.antMatcher("/actuator/metrics")),
                        Arrays.stream(whitelist).map(AntPathRequestMatcher::antMatcher))
                .toArray(AntPathRequestMatcher[]::new);
    }
}
