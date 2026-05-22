package com.koins.loanbackend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Must be a valid phone number")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @Size(min = 11, max = 11, message = "BVN must be exactly 11 digits")
    @Pattern(regexp = "^[0-9]{11}$", message = "BVN must contain only digits")
    private String bvn;

    @Size(min = 11, max = 11, message = "NIN must be exactly 11 digits")
    @Pattern(regexp = "^[0-9]{11}$", message = "NIN must contain only digits")
    private String nin;
}