package com.studioos.server.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.studioos.server.auth.InternalServiceAuthFilter;
import com.studioos.server.auth.JwtAuthFilter;
import com.studioos.server.shared.enums.Role;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalServiceAuthFilter internalServiceAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http

                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // ───────────── AUTH ─────────────

                        .requestMatchers("/auth/register").permitAll()
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers("/auth/otp/resend").permitAll()
                        .requestMatchers("/auth/refresh").permitAll()
                        .requestMatchers("/auth/verification/**").permitAll()
                        .requestMatchers("/auth/password/login").permitAll()
                        .requestMatchers("/auth/password/forgot").permitAll()
                        .requestMatchers("/auth/password/reset").permitAll()

                        .requestMatchers("/auth/logout").authenticated()
                        .requestMatchers("/auth/sessions/**").authenticated()
                        .requestMatchers("/auth/password/change").authenticated()

                        // ───────────── ACTUATOR ─────────────

                        .requestMatchers("/actuator/health").permitAll()

                        // ───────────── PUBLIC CONTENT ─────────────

                        .requestMatchers(HttpMethod.GET, "/beats/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/studios/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/search/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/ads/serve").permitAll()
                        .requestMatchers(HttpMethod.POST, "/ads/*/click").permitAll()

                        // Downloads require login

                        .requestMatchers(HttpMethod.GET, "/beats/*/download")
                        .authenticated()

                        // ───────────── MPESA CALLBACKS ─────────────

                        .requestMatchers("/payment/mpesa/**")
                        .permitAll()

                        // ───────────── INTERNAL SERVICES ─────────────

                        .requestMatchers("/internal/**")
                        .permitAll()

                        // ───────────── ADMIN ─────────────

                        .requestMatchers("/admin/**")
                        .hasRole(Role.SUPER_ADMIN.name())

                        // ───────────── EVERYTHING ELSE ─────────────

                        .anyRequest()
                        .authenticated()

                )

                .addFilterBefore(
                        internalServiceAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .addFilterAfter(
                        jwtAuthFilter,
                        InternalServiceAuthFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(List.of(

                "http://localhost:3000",
                "http://127.0.0.1:3000"

        ));

        configuration.setAllowedMethods(List.of(

                "GET",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"

        ));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();

    }

}