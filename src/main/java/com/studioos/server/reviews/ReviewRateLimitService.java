package com.studioos.server.reviews;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.studioos.server.shared.exceptions.StudioosException;
import com.studioos.server.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReviewRateLimitService {

    private static final long WINDOW_SECONDS = Duration.ofMinutes(1).toSeconds();
    private static final int COMMENT_LIMIT = 5;
    private static final int REACTION_LIMIT = 60;
    private static final String PREFIX = "studioos:review:rate:";

    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
            end
            return count
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public void checkComment(User user) {
        enforce(user, "comment", COMMENT_LIMIT);
    }

    public void checkReaction(User user) {
        enforce(user, "reaction", REACTION_LIMIT);
    }

    private void enforce(User user, String action, int limit) {
        Long count = redisTemplate.execute(
                INCREMENT,
                List.of(PREFIX + action + ":user:" + user.getId()),
                String.valueOf(WINDOW_SECONDS));

        if (count == null || count > limit) {
            throw StudioosException.tooManyRequests("Too many review interactions. Please try again later");
        }
    }
}
