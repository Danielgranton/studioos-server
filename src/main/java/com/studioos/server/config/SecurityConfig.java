package com.studioos.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
                .csrf(AbstractHttpConfigurer -> AbstractHttpConfigurer.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ─── Public endpoints ───
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.GET, "/beats/*/download").authenticated()
                        .requestMatchers(HttpMethod.GET, "/beats/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/studios/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/ads/serve").permitAll()
                        .requestMatchers(HttpMethod.POST, "/ads/*/click").permitAll()
                        .requestMatchers(HttpMethod.GET, "/search/**").permitAll()

                        // ─── M-Pesa callback — Safaricom's servers, no JWT or internal API key possible ───
                        .requestMatchers("/payment/mpesa/**").permitAll()

                        // ─── Internal service-to-service endpoints ───
                        .requestMatchers("/internal/**").permitAll()

                        // ─── Admin only ───
                        .requestMatchers("/admin/**").hasRole(Role.SUPER_ADMIN.name())
                        

                        // ─── Everything else requires auth ───
                        .anyRequest().authenticated()
                )
                .addFilterBefore(internalServiceAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthFilter, InternalServiceAuthFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
