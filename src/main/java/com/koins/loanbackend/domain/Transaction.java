package com.koins.loanbackend.domain;

import com.koins.loanbackend.domain.enums.TransactionStatus;
import com.koins.loanbackend.domain.enums.TransactionType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Wallet getWallet() { return wallet; }
    public void setWallet(Wallet wallet) { this.wallet = wallet; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getNarration() { return narration; }
    public void setNarration(String narration) { this.narration = narration; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}