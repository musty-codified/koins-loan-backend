package com.koins.loanbackend.controller;

import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.Wallet;
import com.koins.loanbackend.domain.enums.TransactionType;
import com.koins.loanbackend.dto.request.FundWalletRequest;
import com.koins.loanbackend.dto.response.TransactionResponse;
import com.koins.loanbackend.dto.response.WalletResponse;
import com.koins.loanbackend.service.TransactionService;
import com.koins.loanbackend.service.UserService;
import com.koins.loanbackend.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
public class WalletController {

    private final WalletService walletService;
    private final TransactionService transactionService;
    private final UserService userService;

    public WalletController(WalletService walletService,
                            TransactionService transactionService,
                            UserService userService) {
        this.walletService = walletService;
        this.transactionService = transactionService;
        this.userService = userService;
    }

    @GetMapping("/me")
    public WalletResponse getMyWallet(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.getByEmail(principal.getUsername());
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        return WalletResponse.from(wallet);
    }

    @PostMapping("/fund")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse fund(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody FundWalletRequest request) {
        User user = userService.getByEmail(principal.getUsername());
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        return transactionService.credit(
            wallet.getId(), user, request.getAmount(),
            TransactionType.Credit, request.getNarration(), idempotencyKey
        );
    }

    @GetMapping("/{walletId}/transactions")
    public Page<TransactionResponse> getTransactions(
            @PathVariable UUID walletId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return transactionService.getWalletTransactions(walletId, pageable);
    }
}