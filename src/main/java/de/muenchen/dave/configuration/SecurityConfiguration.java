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
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

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
                        .requestMatchers(PathPatternRequestMatcher.withDefaults().matcher("/**"))
                        .authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
                .build();
    }

    private PathPatternRequestMatcher[] getPathMatchersForPermitAll() {
        return Stream
                .concat(
                        Stream.of(
                                // allow access to /actuator/info
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/actuator/info"),
                                // allow access to /actuator/health for OpenShift Health Check
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/actuator/health"),
                                // allow access to /actuator/health/liveness for OpenShift Liveness Check
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/actuator/health/liveness"),
                                // allow access to /actuator/health/readiness for OpenShift Readiness Check
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/actuator/health/readiness"),
                                // allow access to /actuator/metrics for Prometheus monitoring in OpenShift
                                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/actuator/metrics")),
                        Arrays.stream(whitelist).map(whitelistUrl -> PathPatternRequestMatcher.withDefaults().matcher(whitelistUrl)))
                .toArray(PathPatternRequestMatcher[]::new);
    }
}
