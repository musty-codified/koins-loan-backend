package com.koins.loanbackend.controller;

import com.koins.loanbackend.dto.request.*;
import com.koins.loanbackend.dto.response.AuthResponse;
import com.koins.loanbackend.dto.response.UserResponse;
import com.koins.loanbackend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }


    @PostMapping("/activate-user")
    public UserResponse activateUser(@Valid @RequestBody ActivateRequest activateUserDto){
        return userService.activateUser(activateUserDto);
    }
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.initiatePasswordReset(request);
    }
    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request);
    }
}