package com.koins.loanbackend.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class FundWalletRequest {

    @NotNull
    @Positive
    private BigDecimal amount;

    private String narration;

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }
}