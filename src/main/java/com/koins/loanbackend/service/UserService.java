package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.Wallet;
import com.koins.loanbackend.domain.enums.OtpPurpose;
import com.koins.loanbackend.domain.enums.UserStatus;
import com.koins.loanbackend.dto.request.*;
import com.koins.loanbackend.dto.response.AuthResponse;
import com.koins.loanbackend.dto.response.UserResponse;
import com.koins.loanbackend.exception.BusinessRuleException;
import com.koins.loanbackend.exception.DuplicateResourceException;
import com.koins.loanbackend.exception.ResourceNotFoundException;
import com.koins.loanbackend.repository.UserRepository;
import com.koins.loanbackend.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository,
                       WalletService walletService,
                       OtpService otpService,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

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
        log.info("===============After UserRepository.save()==============");
        otpService.generateAndStore(user.getEmail(), OtpPurpose.ACCOUNT_ACTIVATION);
        return UserResponse.from(user);
    }

    public UserResponse activateUser(ActivateRequest activate) {
        if (!otpService.validateAndConsume(activate.getEmail().toLowerCase(), OtpPurpose.ACCOUNT_ACTIVATION, activate.getOtp())) {
            throw new BusinessRuleException("OTP is invalid or has expired");
        }
        User user = userRepository.findByEmail(activate.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setStatus(UserStatus.ACTIVE);
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
        String token = jwtTokenProvider.generateToken(auth.getName());
        return new AuthResponse(token, jwtTokenProvider.getExpirationMs());
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

    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail().toLowerCase();
        // OTP validation + atomic consumption — invalid or expired OTP aborts here
        if (!otpService.validateAndConsume(email, OtpPurpose.PASSWORD_RESET, request.getOtp())) {
            throw new BusinessRuleException("OTP is invalid or has expired");
        }
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("No account found for that email address"));
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
}