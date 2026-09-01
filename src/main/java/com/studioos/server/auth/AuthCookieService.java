package com.studioos.server.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import com.studioos.server.auth.dto.AuthResponse;

import jakarta.servlet.http.HttpServletResponse;

@Service
public class AuthCookieService {

    private static final String ACCESS_COOKIE = "studioos_access";
    private static final String REFRESH_COOKIE = "studioos_refresh";

    @Value("${auth.cookies-secure:false}")
    private boolean secure;

    public void addAuthCookies(HttpServletResponse response, AuthResponse auth) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, auth.getAccessToken(), 86400).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, auth.getRefreshToken(), 604800).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, "", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", 0).toString());
    }

    private ResponseCookie cookie(String name, String value, long maxAge) {
        return ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/api/v1")
                .maxAge(maxAge)
                .build();
    }
}
