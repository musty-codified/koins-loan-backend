package com.koins.loanbackend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    // 41 chars = 328 bits — satisfies HS256 minimum of 256 bits
    private static final String TEST_SECRET = "test-jwt-secret-key-for-unit-tests-only!!";

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider();
        ReflectionTestUtils.setField(provider, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(provider, "expirationMs", 3_600_000L);
    }

    @Test
    void generateToken_producesNonBlankJwt() {
        assertThat(provider.generateToken("user@example.com")).isNotBlank();
    }

    @Test
    void getEmailFromToken_returnsOriginalSubject() {
        String token = provider.generateToken("user@example.com");
        assertThat(provider.getEmailFromToken(token)).isEqualTo("user@example.com");
    }

    @Test
    void validateToken_trueForFreshToken() {
        String token = provider.generateToken("user@example.com");
        assertThat(provider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_falseForTamperedSignature() {
        String token = provider.generateToken("user@example.com");
        String tampered = token.substring(0, token.length() - 6) + "XXXXXX";
        assertThat(provider.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_falseForRandomString() {
        assertThat(provider.validateToken("not-a-jwt-token")).isFalse();
    }

    @Test
    void validateToken_falseForExpiredToken() {
        ReflectionTestUtils.setField(provider, "expirationMs", -1_000L);
        String token = provider.generateToken("user@example.com");
        assertThat(provider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_falseForTokenSignedWithDifferentKey() {
        JwtTokenProvider other = new JwtTokenProvider();
        ReflectionTestUtils.setField(other, "secret", "completely-different-secret-key-for-test!!");
        ReflectionTestUtils.setField(other, "expirationMs", 3_600_000L);

        String foreignToken = other.generateToken("user@example.com");
        assertThat(provider.validateToken(foreignToken)).isFalse();
    }
}