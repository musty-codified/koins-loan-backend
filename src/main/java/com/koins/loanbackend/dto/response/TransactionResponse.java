package com.koins.loanbackend.dto.response;

import com.koins.loanbackend.domain.Transaction;
import com.koins.loanbackend.domain.enums.TransactionStatus;
import com.koins.loanbackend.domain.enums.TransactionType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class TransactionResponse {

    private UUID id;
    private UUID walletId;
    private UUID userId;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String reference;
    private String narration;
    private LocalDateTime createdAt;

    public static TransactionResponse from(Transaction tx) {
        TransactionResponse r = new TransactionResponse();
        r.id = tx.getId();
        r.walletId = tx.getWallet().getId();
        r.userId = tx.getUser().getId();
        r.type = tx.getType();
        r.status = tx.getStatus();
        r.amount = tx.getAmount();
        r.reference = tx.getReference();
        r.narration = tx.getNarration();
        r.createdAt = tx.getCreatedAt();
        return r;
    }
}