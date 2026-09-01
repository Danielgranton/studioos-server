package com.studioos.server.auth.otp;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.studioos.server.shared.exceptions.StudioosException;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpRateLimitService {

    private static final long WINDOW_SECONDS = Duration.ofMinutes(10).toSeconds();
    private static final int OTP_REQUEST_LIMIT = 20;
    private static final int VERIFICATION_LIMIT = 50;
    private static final String PREFIX = "studioos:otp-ip:";

    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public void checkOtpRequest(HttpServletRequest request) {
        enforce(request, "request", OTP_REQUEST_LIMIT);
    }

    public void checkVerification(HttpServletRequest request) {
        enforce(request, "verify", VERIFICATION_LIMIT);
    }

    private void enforce(HttpServletRequest request, String action, int limit) {
        String clientIp = request.getRemoteAddr();
        Long count = redisTemplate.execute(
                INCREMENT,
                List.of(PREFIX + action + ":" + clientIp),
                String.valueOf(WINDOW_SECONDS));

        if (count == null || count > limit) {
            throw StudioosException.tooManyRequests(
                    "Too many attempts from this network. Please try again later");
        }
    }
}
