package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.Transaction;
import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.Wallet;
import com.koins.loanbackend.domain.enums.TransactionType;
import com.koins.loanbackend.dto.response.TransactionResponse;
import com.koins.loanbackend.exception.BusinessRuleException;
import com.koins.loanbackend.exception.ResourceNotFoundException;
import com.koins.loanbackend.repository.TransactionRepository;
import com.koins.loanbackend.repository.WalletRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    public TransactionService(TransactionRepository transactionRepository,
                              WalletRepository walletRepository) {
        this.transactionRepository = transactionRepository;
        this.walletRepository = walletRepository;
    }

    public TransactionResponse credit(UUID walletId, User user, BigDecimal amount,
                                      TransactionType type, String narration,
                                      String idempotencyKey) {
        // Fast path: already processed — return cached result without touching balances
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return TransactionResponse.from(existing.get());
        }
        Wallet wallet = lockedWallet(walletId);
        existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return TransactionResponse.from(existing.get());
        }
        wallet.setBalance(wallet.getBalance().add(amount));
        return TransactionResponse.from(
            transactionRepository.save(buildTransaction(wallet, user, type, amount, narration, idempotencyKey))
        );
    }

    public TransactionResponse debit(UUID walletId, User user, BigDecimal amount,
                                     TransactionType type, String narration,
                                     String idempotencyKey) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return TransactionResponse.from(existing.get());
        }
        Wallet wallet = lockedWallet(walletId);

        existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return TransactionResponse.from(existing.get());
        }

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new BusinessRuleException("Insufficient wallet balance");
        }
        wallet.setBalance(wallet.getBalance().subtract(amount));
        return TransactionResponse.from(
            transactionRepository.save(buildTransaction(wallet, user, type, amount, narration, idempotencyKey))
        );
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getWalletTransactions(UUID walletId, Pageable pageable) {
        return transactionRepository
            .findByWalletIdOrderByCreatedAtDesc(walletId, pageable)
            .map(TransactionResponse::from);
    }

    private Wallet lockedWallet(UUID walletId) {
        return walletRepository.findByIdWithLock(walletId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    private Transaction buildTransaction(Wallet wallet, User user, TransactionType type,
                                         BigDecimal amount, String narration, String idempotencyKey) {
        Transaction tx = new Transaction();
        tx.setWallet(wallet);
        tx.setUser(user);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setNarration(narration);
        tx.setIdempotencyKey(idempotencyKey);
        return tx;
    }
}