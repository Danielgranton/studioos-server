package com.studioos.server.auth;

import com.studioos.server.auth.dto.*;
import com.studioos.server.auth.otp.OtpRateLimitService;
import com.studioos.server.shared.dto.ApiResponse;
import com.studioos.server.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final OtpRateLimitService otpRateLimitService;
    private final AuthCookieService authCookieService;
    private final com.studioos.server.auth.service.GoogleOAuthService googleOAuthService;

    @Value("${google.oauth.frontend-success-url:http://localhost:3000/}")
    private String googleSuccessUrl;

    @Value("${google.oauth.frontend-error-url:http://localhost:3000/auth/signin?oauth=error}")
    private String googleErrorUrl;

    @Value("${auth.cookies-secure:false}")
    private boolean secureCookies;

    // ─── Registration ───

    @GetMapping("/oauth2/google")
    public void googleStart(HttpServletResponse response) throws java.io.IOException {
        String state = googleOAuthService.generateState();
        response.addHeader(HttpHeaders.SET_COOKIE, oauthStateCookie(state).toString());
        response.sendRedirect(googleOAuthService.authorizationUrl(state));
    }

    @GetMapping("/oauth2/google/callback")
    public void googleCallback(
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            HttpServletRequest request,
            HttpServletResponse response) throws java.io.IOException {
        String savedState = cookieValue(request, "studioos_google_state");
        response.addHeader(HttpHeaders.SET_COOKIE, clearOauthStateCookie().toString());
        if (error != null || code == null || state == null || savedState == null
                || !MessageDigest.isEqual(state.getBytes(StandardCharsets.UTF_8), savedState.getBytes(StandardCharsets.UTF_8))) {
            response.sendRedirect(googleErrorUrl);
            return;
        }
        try {
            var auth = googleOAuthService.authenticate(code);
            authCookieService.addAuthCookies(response, auth);
            response.sendRedirect(googleSuccessUrl);
        } catch (RuntimeException exception) {
            response.sendRedirect(googleErrorUrl);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<OtpSentResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        otpRateLimitService.checkOtpRequest(httpRequest);
        OtpSentResponse response = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("OTP sent successfully", response));
    }

    // ─── Login ───
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<OtpSentResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        otpRateLimitService.checkOtpRequest(httpRequest);
        OtpSentResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("OTP sent successfully", response));
    }

    // ─── Resend OTP ───
    @PostMapping("/otp/resend")
    public ResponseEntity<ApiResponse<OtpSentResponse>> resendOtp(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        otpRateLimitService.checkOtpRequest(httpRequest);
        OtpSentResponse response = authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.success("OTP resent", response));
    }

    // ─── Refresh token ───
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        RefreshTokenRequest refreshRequest = request == null ? new RefreshTokenRequest() : request;
        if (refreshRequest.getRefreshToken() == null || refreshRequest.getRefreshToken().isBlank()) {
            refreshRequest.setRefreshToken(cookieValue(httpRequest, "studioos_refresh"));
        }
        AuthResponse response = authService.refreshToken(refreshRequest);
        authCookieService.addAuthCookies(httpResponse, response);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LogoutRequest logoutRequest = request == null ? new LogoutRequest() : request;
        if (logoutRequest.getRefreshToken() == null || logoutRequest.getRefreshToken().isBlank()) {
            logoutRequest.setRefreshToken(cookieValue(httpRequest, "studioos_refresh"));
        }
        authService.logout(logoutRequest);
        authCookieService.clearAuthCookies(httpResponse);
        return ResponseEntity.ok(ApiResponse.success("Logged out", null));
    }

    private String cookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) return cookie.getValue();
        }
        return null;
    }

    private ResponseCookie oauthStateCookie(String value) {
        return ResponseCookie.from("studioos_google_state", value).httpOnly(true).secure(secureCookies)
                .path("/").sameSite("Lax").maxAge(java.time.Duration.ofMinutes(5)).build();
    }

    private ResponseCookie clearOauthStateCookie() {
        return ResponseCookie.from("studioos_google_state", "").httpOnly(true).secure(secureCookies)
                .path("/").sameSite("Lax").maxAge(java.time.Duration.ZERO).build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<java.util.List<SessionResponse>>> sessions(@AuthenticationPrincipal User user, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Active sessions", authService.sessions(user, cookieValue(request, "studioos_refresh"))));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @AuthenticationPrincipal User user,
            @PathVariable String sessionId
    ) {
        authService.revokeSession(user, sessionId);
        return ResponseEntity.ok(ApiResponse.success("Session revoked", null));
    }
}
