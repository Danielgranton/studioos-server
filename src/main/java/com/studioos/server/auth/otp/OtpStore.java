package com.studioos.server.auth.otp;

public interface OtpStore {

    void save(String identifier, String codeHash, long ttlSeconds);

    OtpRecord find(String identifier);

    void invalidate(String identifier);

    int incrementRequestCount(String identifier, long ttlSeconds);

    /** Returns the new attempt count, or a negative value when the OTP is unavailable/locked. */
    int recordFailedAttempt(String identifier, String codeHash, long nowEpochSeconds,
                            int maxAttempts, long lockoutSeconds);

    void consume(String identifier, String codeHash);

    record OtpRecord(String codeHash, int failedAttempts, Long lockedUntilEpochSeconds) {}
}
