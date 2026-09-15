package com.studioos.server.notification;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationRateLimitService {

    private static final long WINDOW_SECONDS = Duration.ofMinutes(10).toSeconds();
    private static final int EMAIL_LIMIT = 10;
    private static final int SMS_LIMIT = 5;
    private static final String PREFIX = "studioos:notification:rate:";

    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public boolean allowEmail(User user) {
        return allow(user, "email", EMAIL_LIMIT);
    }

    public boolean allowSms(User user) {
        return allow(user, "sms", SMS_LIMIT);
    }

    private boolean allow(User user, String channel, int limit) {
        try {
            Long count = redisTemplate.execute(
                    INCREMENT,
                    List.of(PREFIX + channel + ":user:" + user.getId()),
                    String.valueOf(WINDOW_SECONDS)
            );

            if (count == null) {
                log.warn("Notification rate limit returned no count for user {} channel {}", user.getId(), channel);
                return true;
            }

            if (count > limit) {
                log.warn("Notification {} rate limit reached for user {}", channel, user.getId());
                return false;
            }

            return true;
        } catch (RuntimeException exception) {
            // Notification throttling must not roll back the business action that created it.
            log.error("Notification rate limiter unavailable; allowing {} notification for user {}", channel, user.getId(), exception);
            return true;
        }
    }
}
