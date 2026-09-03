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
    private static final String LEGACY_COOKIE_PATH = "/api/v1";

    @Value("${auth.cookies-secure:false}")
    private boolean secure;

    @Value("${auth.cookie-path:/}")
    private String cookiePath;

    public void addAuthCookies(HttpServletResponse response, AuthResponse auth) {
        expireLegacyCookies(response);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, auth.getAccessToken(), 86400, cookiePath).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, auth.getRefreshToken(), 604800, cookiePath).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, "", 0, cookiePath).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", 0, cookiePath).toString());
        expireLegacyCookies(response);
    }

    private void expireLegacyCookies(HttpServletResponse response) {
        if (LEGACY_COOKIE_PATH.equals(cookiePath)) {
            return;
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(ACCESS_COOKIE, "", 0, LEGACY_COOKIE_PATH).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(REFRESH_COOKIE, "", 0, LEGACY_COOKIE_PATH).toString());
    }

    private ResponseCookie cookie(String name, String value, long maxAge, String path) {
        return ResponseCookie.from(name, value == null ? "" : value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
    }
}
