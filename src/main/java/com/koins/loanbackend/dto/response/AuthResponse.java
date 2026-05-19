package com.koins.loanbackend.dto.response;

public class AuthResponse {

    private final String accessToken;
    private final String tokenType = "Bearer";
    private final long expiresIn;

    public AuthResponse(String accessToken, long expiresIn) {
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() { return tokenType; }
    public long getExpiresIn() { return expiresIn; }
}