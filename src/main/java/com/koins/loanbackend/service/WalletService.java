package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.Wallet;
import com.koins.loanbackend.exception.ResourceNotFoundException;
import com.koins.loanbackend.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class WalletService {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    public Wallet createForUser(User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user"));
    }

    @Transactional(readOnly = true)
    public Wallet getWalletById(UUID walletId) {
        return walletRepository.findById(walletId)
            .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }
}
