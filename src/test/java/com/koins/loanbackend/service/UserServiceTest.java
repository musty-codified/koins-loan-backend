package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.Wallet;
import com.koins.loanbackend.domain.enums.OtpPurpose;
import com.koins.loanbackend.domain.enums.UserStatus;
import com.koins.loanbackend.domain.enums.WalletStatus;
import com.koins.loanbackend.dto.request.ActivateRequest;
import com.koins.loanbackend.dto.request.RegisterRequest;
import com.koins.loanbackend.dto.response.UserResponse;
import com.koins.loanbackend.exception.BusinessRuleException;
import com.koins.loanbackend.exception.DuplicateResourceException;
import com.koins.loanbackend.exception.ResourceNotFoundException;
import com.koins.loanbackend.repository.UserRepository;
import com.koins.loanbackend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock WalletService walletService;
    @Mock OtpService otpService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock AuthenticationManager authenticationManager;

    @InjectMocks UserService userService;

    private RegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterRequest();
        validRequest.setName("Jane Doe");
        validRequest.setEmail("jane@example.com");
        validRequest.setPhone("08012345678");
        validRequest.setPassword("SecurePass1!");
        validRequest.setBvn("12345678901");
        validRequest.setNin("98765432101");
    }

    // =========================================================================
    // register — user saved as INACTIVE, no wallet, activation OTP dispatched
    // =========================================================================

    @Test
    void register_savesUserAsInactive_withNoWallet() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inactiveUser(inv.getArgument(0)));

        UserResponse response = userService.register(validRequest);

        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        assertThat(response.getStatus()).isEqualTo(UserStatus.INACTIVE);
        assertThat(response.getWallet()).isNull();
    }

    @Test
    void register_firesAccountActivationOtp_notWalletCreation() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inactiveUser(inv.getArgument(0)));

        userService.register(validRequest);

        verify(otpService).generateAndStore("jane@example.com", OtpPurpose.ACCOUNT_ACTIVATION);
        verify(walletService, never()).createForUser(any());
    }

    @Test
    void register_passwordIsHashed_notStoredInPlainText() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode("SecurePass1!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inactiveUser(inv.getArgument(0)));

        userService.register(validRequest);

        verify(passwordEncoder).encode("SecurePass1!");
        verify(userRepository).save(argThat(u -> "bcrypt-hash".equals(u.getPassword())));
    }

    @Test
    void register_normalisesEmailToLowercase() {
        validRequest.setEmail("JANE@EXAMPLE.COM");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inactiveUser(inv.getArgument(0)));

        UserResponse response = userService.register(validRequest);

        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        verify(userRepository).save(argThat(u -> "jane@example.com".equals(u.getEmail())));
    }

    @Test
    void register_throwsDuplicateException_whenEmailTaken() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Email");

        verify(userRepository, never()).save(any());
        verify(otpService, never()).generateAndStore(any(), any());
        verify(walletService, never()).createForUser(any());
    }

    @Test
    void register_throwsDuplicateException_whenPhoneTaken() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone("08012345678")).thenReturn(true);

        assertThatThrownBy(() -> userService.register(validRequest))
            .isInstanceOf(DuplicateResourceException.class)
            .hasMessageContaining("Phone");

        verify(userRepository, never()).save(any());
        verify(otpService, never()).generateAndStore(any(), any());
    }

    @Test
    void register_skipsPhoneCheck_whenPhoneIsNull() {
        validRequest.setPhone(null);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inactiveUser(inv.getArgument(0)));

        assertThatCode(() -> userService.register(validRequest)).doesNotThrowAnyException();
        verify(userRepository, never()).existsByPhone(any());
    }

    // =========================================================================
    // activateUser — OTP validated, user set ACTIVE, wallet created
    // =========================================================================

    @Test
    void activateUser_setsUserActiveAndCreatesWallet_whenOtpValid() {
        ActivateRequest request = activateRequest("jane@example.com", "123456");

        when(otpService.validateAndConsume("jane@example.com", OtpPurpose.ACCOUNT_ACTIVATION, "123456"))
            .thenReturn(true);

        User savedUser = inactiveUser(new User());
        savedUser.setEmail("jane@example.com");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(savedUser));
        when(walletService.createForUser(savedUser)).thenReturn(activeWallet());

        UserResponse response = userService.activateUser(request);

        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.getWallet()).isNotNull();
        assertThat(response.getWallet().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getWallet().getCurrency()).isEqualTo("NGN");
        verify(walletService).createForUser(savedUser);
    }

    @Test
    void activateUser_throwsBusinessRuleException_whenOtpInvalidOrExpired() {
        ActivateRequest request = activateRequest("jane@example.com", "000000");

        when(otpService.validateAndConsume("jane@example.com", OtpPurpose.ACCOUNT_ACTIVATION, "000000"))
            .thenReturn(false);

        assertThatThrownBy(() -> userService.activateUser(request))
            .isInstanceOf(BusinessRuleException.class)
            .hasMessageContaining("OTP");

        verify(userRepository, never()).findByEmail(any());
        verify(walletService, never()).createForUser(any());
    }

    @Test
    void activateUser_throwsResourceNotFoundException_whenUserNotFound() {
        ActivateRequest request = activateRequest("ghost@example.com", "123456");

        when(otpService.validateAndConsume("ghost@example.com", OtpPurpose.ACCOUNT_ACTIVATION, "123456"))
            .thenReturn(true);
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.activateUser(request))
            .isInstanceOf(ResourceNotFoundException.class);

        verify(walletService, never()).createForUser(any());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private User inactiveUser(User user) {
        if (user == null) user = new User();
        user.setStatus(UserStatus.INACTIVE);
        return user;
    }

    private Wallet activeWallet() {
        Wallet w = new Wallet();
        w.setBalance(BigDecimal.ZERO.setScale(2));
        w.setCurrency("NGN");
        w.setStatus(WalletStatus.ACTIVE);
        return w;
    }

    private ActivateRequest activateRequest(String email, String otp) {
        ActivateRequest r = new ActivateRequest();
        r.setEmail(email);
        r.setOtp(otp);
        return r;
    }
}
