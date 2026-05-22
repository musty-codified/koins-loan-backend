package com.koins.loanbackend.controller;

import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.dto.request.UpdateProfileRequest;
import com.koins.loanbackend.dto.response.UserResponse;
import com.koins.loanbackend.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Users", description = "Profile management and logout")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            userService.logout(authHeader.substring(7));
        }
    }

    @PatchMapping("/me")
    public UserResponse updateProfile(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        User user = userService.getByEmail(principal.getUsername());
        return userService.updateProfile(user, request);
    }
}