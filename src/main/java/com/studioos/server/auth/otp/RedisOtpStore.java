package com.studioos.server.auth.otp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RedisOtpStore implements OtpStore {

    private static final String PREFIX = "studioos:otp:";
    private static final String REQUEST_PREFIX = "studioos:otp-requests:";
    private static final String CODE_HASH = "codeHash";
    private static final String ATTEMPTS = "failedAttempts";
    private static final String LOCKED_UNTIL = "lockedUntil";

    private static final DefaultRedisScript<Long> RECORD_FAILURE = new DefaultRedisScript<>("""
            local current = redis.call('HGET', KEYS[1], 'codeHash')
            if not current or current ~= ARGV[1] then return -1 end
            local locked = redis.call('HGET', KEYS[1], 'lockedUntil')
            local now = tonumber(ARGV[2])
            if locked and tonumber(locked) > now then return -2 end
            local attempts = redis.call('HINCRBY', KEYS[1], 'failedAttempts', 1)
            if attempts >= tonumber(ARGV[3]) then
              redis.call('HSET', KEYS[1], 'lockedUntil', now + tonumber(ARGV[4]))
            end
            return attempts
            """, Long.class);

    private static final DefaultRedisScript<Long> INCREMENT_REQUESTS = new DefaultRedisScript<>("""
            local count = redis.call('INCR', KEYS[1])
            if count == 1 then
              redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
            end
            return count
            """, Long.class);

    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            local current = redis.call('HGET', KEYS[1], 'codeHash')
            if current and current == ARGV[1] then
              return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public void save(String identifier, String codeHash, long ttlSeconds) {
        String key = key(identifier);
        Map<String, String> values = new HashMap<>();
        values.put(CODE_HASH, codeHash);
        values.put(ATTEMPTS, "0");
        redisTemplate.delete(key);
        redisTemplate.opsForHash().putAll(key, values);
        redisTemplate.expire(key, java.time.Duration.ofSeconds(ttlSeconds));
    }

    @Override
    public OtpRecord find(String identifier) {
        Map<Object, Object> values = redisTemplate.opsForHash().entries(key(identifier));
        if (values.isEmpty() || values.get(CODE_HASH) == null) {
            return null;
        }
        String lockedUntil = value(values.get(LOCKED_UNTIL));
        return new OtpRecord(
                value(values.get(CODE_HASH)),
                Integer.parseInt(value(values.getOrDefault(ATTEMPTS, "0"))),
                lockedUntil == null ? null : Long.valueOf(lockedUntil));
    }

    @Override
    public void invalidate(String identifier) {
        redisTemplate.delete(key(identifier));
    }

    @Override
    public int incrementRequestCount(String identifier, long ttlSeconds) {
        Long result = redisTemplate.execute(
                INCREMENT_REQUESTS,
                List.of(requestKey(identifier)),
                String.valueOf(ttlSeconds));
        return result == null ? Integer.MAX_VALUE : result.intValue();
    }

    @Override
    public int recordFailedAttempt(String identifier, String codeHash, long nowEpochSeconds,
                                   int maxAttempts, long lockoutSeconds) {
        Long result = redisTemplate.execute(
                RECORD_FAILURE,
                List.of(key(identifier)),
                codeHash,
                String.valueOf(nowEpochSeconds),
                String.valueOf(maxAttempts),
                String.valueOf(lockoutSeconds));
        return result == null ? -1 : result.intValue();
    }

    @Override
    public void consume(String identifier, String codeHash) {
        redisTemplate.execute(CONSUME, List.of(key(identifier)), codeHash);
    }

    private String key(String identifier) {
        return PREFIX + sha256(identifier.trim().toLowerCase());
    }

    private String requestKey(String identifier) {
        return REQUEST_PREFIX + sha256(identifier.trim().toLowerCase());
    }

    private String value(Object value) {
        return value == null ? null : value.toString();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
