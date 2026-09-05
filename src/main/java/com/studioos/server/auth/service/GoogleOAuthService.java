package com.studioos.server.auth.service;

import java.net.URI;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioos.server.auth.AuthService;
import com.studioos.server.auth.dto.AuthResponse;
import com.studioos.server.shared.exceptions.StudioosException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_ENDPOINT = "https://openidconnect.googleapis.com/v1/userinfo";

    private final AuthService authService;
    private final ObjectMapper objectMapper;
    private final RestClient.Builder restClientBuilder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${google.oauth.enabled:false}")
    private boolean enabled;
    @Value("${google.oauth.client-id:}")
    private String clientId;
    @Value("${google.oauth.client-secret:}")
    private String clientSecret;
    @Value("${google.oauth.redirect-uri:http://localhost:8080/api/v1/auth/oauth2/google/callback}")
    private String redirectUri;

    public String authorizationUrl(String state) {
        ensureConfigured();
        return UriComponentsBuilder.fromUriString(AUTHORIZATION_ENDPOINT)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("access_type", "online")
                .queryParam("prompt", "select_account")
                .build().encode().toUriString();
    }

    public AuthResponse authenticate(String code) {
        ensureConfigured();
        try {
            String tokenResponse = restClientBuilder.build().post().uri(TOKEN_ENDPOINT)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body("code=" + encode(code) + "&client_id=" + encode(clientId) + "&client_secret=" + encode(clientSecret) + "&redirect_uri=" + encode(redirectUri) + "&grant_type=authorization_code")
                    .retrieve().body(String.class);
            JsonNode token = objectMapper.readTree(tokenResponse);
            String accessToken = token.path("access_token").asText(null);
            if (accessToken == null) throw StudioosException.unauthorized("Google authorization failed");

            String userResponse = restClientBuilder.build().get().uri(USERINFO_ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve().body(String.class);
            JsonNode user = objectMapper.readTree(userResponse);
            String subject = user.path("sub").asText(null);
            String email = user.path("email").asText(null);
            if (subject == null || email == null || !user.path("email_verified").asBoolean(false)) {
                throw StudioosException.unauthorized("Google account email is not verified");
            }
            return authService.googleLogin(subject, email.toLowerCase(), user.path("name").asText(email));
        } catch (StudioosException exception) {
            throw exception;
        } catch (Exception exception) {
            throw StudioosException.unauthorized("Google sign-in failed");
        }
    }

    public String generateState() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void ensureConfigured() {
        if (!enabled || clientId.isBlank() || clientSecret.isBlank()) {
            throw StudioosException.badRequest("Google sign-in is not configured");
        }
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
