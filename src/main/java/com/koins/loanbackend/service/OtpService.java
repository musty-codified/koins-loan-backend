package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.enums.OtpPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    static final long TTL_SECONDS = 300; // 5 minutes
    private static final int LOWER_BOUND = 100_000;
    private static final int RANGE = 900_000;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Generates a 6-digit OTP, stores it in Redis with a 5-minute TTL,
     * and logs it to the console.
     */
    public void generateAndStore(String identifier, OtpPurpose purpose) {
        String otp = String.valueOf(LOWER_BOUND + secureRandom.nextInt(RANGE));
        String key = buildKey(purpose, identifier);
        redisTemplate.opsForValue().set(key, otp, Duration.ofSeconds(TTL_SECONDS));

        log.info("\n========================================" +
                 "\n[DEV OTP] purpose   : {}" +
                 "\n[DEV OTP] identifier: {}" +
                 "\n[DEV OTP] code      : {}" +
                 "\n[DEV OTP] expires in: 5 minutes" +
                 "\n========================================",
                purpose, identifier, otp);
    }

    /**
     * Validates the submitted OTP against the stored value.
     * Deletes the key on a successful match.
     */
    public boolean validateAndConsume(String identifier, OtpPurpose purpose, String submitted) {
        String key = buildKey(purpose, identifier);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored != null && stored.equals(submitted)) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    static String buildKey(OtpPurpose purpose, String identifier) {
        return "otp:" + purpose.name().toLowerCase() + ":" + identifier;
    }


}