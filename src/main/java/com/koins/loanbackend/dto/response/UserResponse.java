package com.koins.loanbackend.dto.response;

import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String name;
    private String email;
    private String phone;
    private UserStatus status;
    private LocalDateTime createdAt;
    private WalletResponse wallet;

    public static UserResponse from(User user) {
        UserResponse r = new UserResponse();
        r.id = user.getId();
        r.name = user.getName();
        r.email = user.getEmail();
        r.phone = user.getPhone();
        r.status = user.getStatus();
        r.createdAt = user.getCreatedAt();
        if (user.getWallet() != null) {
            r.wallet = WalletResponse.from(user.getWallet());
        }
        return r;
    }


    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public WalletResponse getWallet() {
        return wallet;
    }
}
