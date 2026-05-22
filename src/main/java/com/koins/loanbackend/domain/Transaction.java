package com.koins.loanbackend.domain;

import com.koins.loanbackend.domain.enums.TransactionStatus;
import com.koins.loanbackend.domain.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "transactions", indexes = {
    @Index(name = "idx_txn_wallet_id",       columnList = "wallet_id"),
    @Index(name = "idx_txn_user_id",         columnList = "user_id"),
    @Index(name = "idx_txn_reference",       columnList = "reference",       unique = true),
    @Index(name = "idx_txn_idempotency_key", columnList = "idempotency_key", unique = true)
})
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    private String narration;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64, updatable = false)
    private String idempotencyKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null) status = TransactionStatus.SUCCESS;
        if (reference == null) {
            reference = "TXN-" + System.currentTimeMillis() + "-"
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        }
        if (idempotencyKey == null) {
            idempotencyKey = UUID.randomUUID().toString();
        }
    }
}