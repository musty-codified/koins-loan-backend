package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.Wallet;
import com.koins.loanbackend.domain.enums.AccountStatus;
import com.koins.loanbackend.domain.enums.OtpPurpose;
import com.koins.loanbackend.dto.request.*;
import com.koins.loanbackend.dto.response.AuthResponse;
import com.koins.loanbackend.dto.response.UserResponse;
import com.koins.loanbackend.exception.BusinessRuleException;
import com.koins.loanbackend.exception.DuplicateResourceException;
import com.koins.loanbackend.exception.ResourceNotFoundException;
import com.koins.loanbackend.repository.UserRepository;
import com.koins.loanbackend.security.JwtTokenProvider;
import com.koins.loanbackend.security.TokenBlocklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final TokenBlocklistService tokenBlocklistService;

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email {" + request.getEmail() + "} is already registered");
        }
        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("Phone number is already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPhone(request.getPhone());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setBvn(request.getBvn());
        user.setNin(request.getNin());
        user = userRepository.save(user);
        otpService.generateAndStore(user.getEmail(), OtpPurpose.ACCOUNT_ACTIVATION);
        return UserResponse.from(user);
    }

    public UserResponse activateUser(ActivateRequest activate) {
        if (!otpService.validateAndConsume(activate.getEmail().toLowerCase(), OtpPurpose.ACCOUNT_ACTIVATION, activate.getOtp())) {
            throw new BusinessRuleException("OTP is invalid or has expired");
        }
        User user = userRepository.findByEmail(activate.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(AccountStatus.ACTIVE);
        Wallet wallet = walletService.createForUser(user);
        user.setWallet(wallet);
        return UserResponse.from(user);
    }

    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase(),
                        request.getPassword()
                )
        );

        if (auth.isAuthenticated()) {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid login credential"));
            if (!user.getStatus().equals(AccountStatus.ACTIVE)) {
                throw new BusinessRuleException("User not active. Please activate your account");
            }
            log.info("Generating access token for {}", user.getEmail());
            String token = jwtTokenProvider.generateToken(auth.getName());
            return new AuthResponse(token, jwtTokenProvider.getExpirationMs());
        }
        throw new BadCredentialsException("Invalid username or password");
    }

    public void initiatePasswordReset(ForgotPasswordRequest request) {
        String email = request.getEmail().toLowerCase();
        userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for that email address"));
        otpService.generateAndStore(email, OtpPurpose.PASSWORD_RESET);
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public void logout(String token) {
        long ttl = jwtTokenProvider.getRemainingTtlSeconds(token);
        tokenBlocklistService.revoke(token, ttl);
    }

    public UserResponse updateProfile(User user, UpdateProfileRequest request) {
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new DuplicateResourceException("Phone number is already registered");
            }
            user.setPhone(request.getPhone());
        }
        user.setName(request.getName());
        return UserResponse.from(userRepository.save(user));
    }

    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase();
        if (!otpService.validateAndConsume(email, OtpPurpose.PASSWORD_RESET, request.getOtp())) {
            throw new BusinessRuleException("OTP is invalid or has expired");
        }
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found for that email address"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}