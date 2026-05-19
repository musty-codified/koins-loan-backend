package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.Wallet;
import com.koins.loanbackend.domain.enums.UserStatus;
import com.koins.loanbackend.dto.request.LoginRequest;
import com.koins.loanbackend.dto.request.RegisterRequest;
import com.koins.loanbackend.dto.response.AuthResponse;
import com.koins.loanbackend.dto.response.UserResponse;
import com.koins.loanbackend.exception.DuplicateResourceException;
import com.koins.loanbackend.exception.ResourceNotFoundException;
import com.koins.loanbackend.exception.UnauthorizedException;
import com.koins.loanbackend.repository.UserRepository;
import com.koins.loanbackend.security.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final WalletService walletService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository, WalletService walletService,
                       PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.walletService = walletService;
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

        Wallet wallet = walletService.createForUser(user);
        user.setWallet(wallet);

        return UserResponse.from(user);
    }


    public AuthResponse login(LoginRequest loginRequest) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail().toLowerCase(),
                        loginRequest.getPassword()
                )
        );
        String token = jwtTokenProvider.generateToken(auth.getName());
        log.info("Generated Token : {}", token);
        return new AuthResponse(token, jwtTokenProvider.getExpirationMs());
    }
}
