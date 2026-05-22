package com.koins.loanbackend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlocklistService {

    private static final String PREFIX = "jwt:blocklist:";

    private final RedisTemplate<String, String> redisTemplate;

    public void revoke(String token, long ttlSeconds) {
        if (ttlSeconds > 0) {
            redisTemplate.opsForValue().set(PREFIX + token, "1", ttlSeconds, TimeUnit.SECONDS);
        }
    }

    public boolean isRevoked(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
    }
}