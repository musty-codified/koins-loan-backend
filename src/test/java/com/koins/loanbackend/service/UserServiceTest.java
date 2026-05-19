package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.Wallet;
import com.koins.loanbackend.domain.enums.UserStatus;
import com.koins.loanbackend.domain.enums.WalletStatus;
import com.koins.loanbackend.dto.request.RegisterRequest;
import com.koins.loanbackend.dto.response.UserResponse;
import com.koins.loanbackend.exception.DuplicateResourceException;
import com.koins.loanbackend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock WalletService walletService;
    @Mock PasswordEncoder passwordEncoder;

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

    @Test
    void register_createsUserAndAttachesWallet() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-password");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> stubPersistedUser(inv.getArgument(0)));
        when(walletService.createForUser(any(User.class))).thenReturn(stubWallet());

        UserResponse response = userService.register(validRequest);

        assertThat(response.getEmail()).isEqualTo("jane@example.com");
        assertThat(response.getWallet()).isNotNull();
        assertThat(response.getWallet().getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(response.getWallet().getCurrency()).isEqualTo("NGN");
        assertThat(response.getWallet().getStatus()).isEqualTo(WalletStatus.ACTIVE);

        verify(walletService).createForUser(any(User.class));
    }

    @Test
    void register_passwordIsHashed_notStoredInPlainText() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode("SecurePass1!")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> stubPersistedUser(inv.getArgument(0)));
        when(walletService.createForUser(any())).thenReturn(stubWallet());

        userService.register(validRequest);

        verify(passwordEncoder).encode("SecurePass1!");
        // verifies the raw password never reaches the repository
        verify(userRepository).save(argThat(u -> "bcrypt-hash".equals(u.getPassword())));
    }

    @Test
    void register_normalisesEmailToLowercase() {
        validRequest.setEmail("JANE@EXAMPLE.COM");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByPhone(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> stubPersistedUser(inv.getArgument(0)));
        when(walletService.createForUser(any())).thenReturn(stubWallet());

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
    }

    @Test
    void register_skipsPhoneCheck_whenPhoneIsNull() {
        validRequest.setPhone(null);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> stubPersistedUser(inv.getArgument(0)));
        when(walletService.createForUser(any())).thenReturn(stubWallet());

        assertThatCode(() -> userService.register(validRequest)).doesNotThrowAnyException();

        verify(userRepository, never()).existsByPhone(any());
    }

    // --- helpers ---

    private User stubPersistedUser(User user) {
        // simulate @PrePersist side-effects without JPA
        if (user.getStatus() == null) user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Wallet stubWallet() {
        Wallet w = new Wallet();
        // simulate @PrePersist defaults (JPA lifecycle not invoked in unit tests)
        w.setBalance(BigDecimal.ZERO.setScale(2));
        w.setCurrency("NGN");
        w.setStatus(WalletStatus.ACTIVE);
        return w;
    }
}
