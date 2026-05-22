package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.enums.OtpPurpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    static final long TTL_SECONDS = 300;
    private static final int LOWER_BOUND = 100_000;
    private static final int RANGE = 900_000;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

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