
package com.hoakhanh.rental;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        JwtGrantedAuthoritiesConverter authoritiesConverter =
                new JwtGrantedAuthoritiesConverter();

        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter jwtConverter =
                new JwtAuthenticationConverter();

        jwtConverter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )
                .authorizeHttpRequests(auth -> auth


                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/landlord/rooms/*/photos/*/file"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/landlord/rooms/*/photos"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/landlord/rooms/*/photos"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/landlord/rooms/*/photos/*"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/rooms/*/photos",
                                "/uploads/rooms/**"
                        ).permitAll()

                        .requestMatchers(
                                "/api/v1/admin/**"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/landlord/rooms/*/status"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/v1/landlord/rooms/*"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/v1/landlord/rooms/*"
                        ).authenticated()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/hello",
                                "/api/v1/rooms",
                                "/api/v1/rooms/*"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/landlord/rooms"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/auth/me"
                        ).authenticated()

                        // Contact requests
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/rooms/*/contact-requests"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/contact-requests/mine"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/v1/landlord/contact-requests"
                        ).authenticated()

                        .requestMatchers(
                                HttpMethod.PATCH,
                                "/api/v1/landlord/contact-requests/*/status"
                        ).authenticated()

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtConverter
                                )
                        )
                )
                .build();
    }
}
