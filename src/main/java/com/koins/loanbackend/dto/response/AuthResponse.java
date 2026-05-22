package com.koins.loanbackend.dto.response;

import lombok.Getter;

@Getter
public class AuthResponse {

    private final String accessToken;
    private final String tokenType = "Bearer";
    private final long expiresIn;

    public AuthResponse(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }
}
