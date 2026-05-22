package com.koins.loanbackend.controller;

import com.koins.loanbackend.domain.Loan;
import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.dto.request.LoanApplicationRequest;
import com.koins.loanbackend.dto.request.LoanApproveRequest;
import com.koins.loanbackend.dto.request.RepayLoanRequest;
import com.koins.loanbackend.dto.response.LoanResponse;
import com.koins.loanbackend.dto.response.RepaymentScheduleResponse;
import com.koins.loanbackend.service.LoanService;
import com.koins.loanbackend.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanService loanService;
    private final UserService userService;

    private final Logger log = LoggerFactory.getLogger(LoanController.class);
    public LoanController(LoanService loanService, UserService userService) {
        this.loanService = loanService;
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse apply(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody LoanApplicationRequest request) {
        User user = userService.getByEmail(principal.getUsername());
        Loan loan = loanService.apply(user, request.getLoanAmount(), request.getTenureMonths());
        return LoanResponse.from(loan);
    }

    @GetMapping("/me")
    public List<LoanResponse> getMyLoans(@AuthenticationPrincipal UserDetails principal) {
        User user = userService.getByEmail(principal.getUsername());
        return loanService.getUserLoans(user.getId())
            .stream()
            .map(LoanResponse::from)
            .toList();
    }

    @GetMapping("/{loanId}")
    public LoanResponse getLoan(
            @PathVariable UUID loanId,
            @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getByEmail(principal.getUsername());
        return LoanResponse.from(loanService.getLoan(loanId, user));
    }

    @GetMapping("/{loanId}/schedule")
    public List<RepaymentScheduleResponse> getSchedule(
            @PathVariable UUID loanId,
            @AuthenticationPrincipal UserDetails principal) {
        User user = userService.getByEmail(principal.getUsername());
        // Ownership verified via getLoan — throws 404 if loan doesn't belong to user
        loanService.getLoan(loanId, user);
        return loanService.getSchedule(loanId)
            .stream()
            .map(RepaymentScheduleResponse::from)
            .toList();
    }

    @PostMapping("/{loanId}/repay")
    public LoanResponse repay(
            @PathVariable UUID loanId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody RepayLoanRequest request) {
        User user = userService.getByEmail(principal.getUsername());
        LoanService.LoanRepaymentResult result = loanService.repay(
            loanId, request.getAmount(), user, idempotencyKey);
        return LoanResponse.from(result.loan());
    }

    // ── Administrative routes ──────────────────────────────────────────────────

    @PostMapping("/{loanId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public LoanResponse approve(
            @PathVariable UUID loanId,
            @RequestBody(required = false) LoanApproveRequest request) {
        log.info("Loan approval handler triggered");
        LoanApproveRequest effectiveRequest = request != null ? request : new LoanApproveRequest();
        Loan loan = loanService.approveLoan(loanId, effectiveRequest.getAmortizationMethod());
        return LoanResponse.from(loan);
    }

    @PostMapping("/{loanId}/disburse")
    @PreAuthorize("hasRole('ADMIN')")
    public LoanResponse disburse(
            @PathVariable UUID loanId,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return LoanResponse.from(loanService.disburseLoan(loanId, idempotencyKey));
    }
}