package com.koins.loanbackend.repository;

import com.koins.loanbackend.domain.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    Page<Transaction> findByWalletIdOrderByCreatedAtDesc(UUID walletId, Pageable pageable);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
}