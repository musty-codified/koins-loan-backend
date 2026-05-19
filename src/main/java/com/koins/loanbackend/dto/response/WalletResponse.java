package com.koins.loanbackend.dto.response;

import com.koins.loanbackend.domain.Wallet;
import com.koins.loanbackend.domain.enums.WalletStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class WalletResponse {

    private UUID id;
    private BigDecimal balance;
    private String currency;
    private WalletStatus status;
    private LocalDateTime createdAt;

    public static WalletResponse from(Wallet wallet) {
        WalletResponse r = new WalletResponse();
        r.id = wallet.getId();
        r.balance = wallet.getBalance();
        r.currency = wallet.getCurrency();
        r.status = wallet.getStatus();
        r.createdAt = wallet.getCreatedAt();
        return r;
    }

    public UUID getId() { return id; }
    public BigDecimal getBalance() { return balance; }
    public String getCurrency() { return currency; }
    public WalletStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
