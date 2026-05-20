package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.enums.OtpPurpose;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks OtpService otpService;

    // =========================================================================
    // generateAndStore
    // =========================================================================

    @Test
    void generateAndStore_writesStructuredKeyWithFiveMinuteTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        otpService.generateAndStore("user@test.com", OtpPurpose.PASSWORD_RESET);

        verify(valueOps).set(
            eq("otp:password_reset:user@test.com"),
            argThat(otp -> otp.matches("\\d{6}")),
            eq(Duration.ofSeconds(OtpService.TTL_SECONDS))
        );
    }

    @Test
    void generateAndStore_alwaysProducesSixDigitNumericOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        for (int i = 0; i < 30; i++) {
            otpService.generateAndStore("user@test.com", OtpPurpose.ACCOUNT_ACTIVATION);
        }

        verify(valueOps, times(30)).set(
            anyString(),
            argThat(otp -> otp.length() == 6 && otp.chars().allMatch(Character::isDigit)),
            any(Duration.class)
        );
    }

    // =========================================================================
    // validateAndConsume
    // =========================================================================

    @Test
    void validateAndConsume_returnsTrueAndDeletesKey_onMatch() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("otp:password_reset:user@test.com")).thenReturn("123456");

        boolean result = otpService.validateAndConsume("user@test.com", OtpPurpose.PASSWORD_RESET, "123456");

        assertThat(result).isTrue();
        verify(redisTemplate).delete("otp:password_reset:user@test.com");
    }

    @Test
    void validateAndConsume_returnsFalse_onMismatch() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("otp:password_reset:user@test.com")).thenReturn("111111");

        boolean result = otpService.validateAndConsume("user@test.com", OtpPurpose.PASSWORD_RESET, "999999");

        assertThat(result).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void validateAndConsume_returnsFalse_whenKeyExpiredOrAbsent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(anyString())).thenReturn(null);

        assertThat(otpService.validateAndConsume("user@test.com", OtpPurpose.PASSWORD_RESET, "123456")).isFalse();
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void validateAndConsume_isOtpSingleUse_keyDeletedAfterSuccess() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("otp:account_activation:user@test.com")).thenReturn("654321");

        otpService.validateAndConsume("user@test.com", OtpPurpose.ACCOUNT_ACTIVATION, "654321");

        // Key must be deleted so the same OTP cannot be replayed
        verify(redisTemplate, times(1)).delete("otp:account_activation:user@test.com");
    }

    // =========================================================================
    // buildKey — pure logic, no Redis involved
    // =========================================================================

    @Test
    void buildKey_followsOtpPurposeIdentifierFormat() {
        assertThat(OtpService.buildKey(OtpPurpose.PASSWORD_RESET, "user@test.com"))
            .isEqualTo("otp:password_reset:user@test.com");

        assertThat(OtpService.buildKey(OtpPurpose.PHONE_VERIFICATION, "+2348012345678"))
            .isEqualTo("otp:phone_verification:+2348012345678");

        assertThat(OtpService.buildKey(OtpPurpose.ACCOUNT_ACTIVATION, "user@test.com"))
            .isEqualTo("otp:account_activation:user@test.com");
    }

    @Test
    void buildKey_scopedByPurpose_preventsCrossFlowCollisions() {
        String resetKey      = OtpService.buildKey(OtpPurpose.PASSWORD_RESET,     "same@email.com");
        String verifyKey     = OtpService.buildKey(OtpPurpose.PHONE_VERIFICATION,  "same@email.com");
        String activationKey = OtpService.buildKey(OtpPurpose.ACCOUNT_ACTIVATION,  "same@email.com");

        assertThat(resetKey).isNotEqualTo(verifyKey);
        assertThat(resetKey).isNotEqualTo(activationKey);
        assertThat(verifyKey).isNotEqualTo(activationKey);
    }
}