package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.Loan;
import com.koins.loanbackend.domain.RepaymentSchedule;
import com.koins.loanbackend.domain.User;
import com.koins.loanbackend.domain.Wallet;
import com.koins.loanbackend.domain.enums.AmortizationMethod;
import com.koins.loanbackend.domain.enums.LoanStatus;
import com.koins.loanbackend.domain.enums.RepaymentScheduleStatus;
import com.koins.loanbackend.domain.enums.TransactionType;
import com.koins.loanbackend.exception.BusinessRuleException;
import com.koins.loanbackend.exception.ResourceNotFoundException;
import com.koins.loanbackend.repository.LoanRepository;
import com.koins.loanbackend.repository.RepaymentScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LoanService {

    private static final BigDecimal DEFAULT_INTEREST_RATE = new BigDecimal("0.1500");
    private static final AmortizationMethod DEFAULT_METHOD = AmortizationMethod.REDUCING_BALANCE;

    private final LoanRepository loanRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanAmortizationService amortizationService;
    private final TransactionService transactionService;
    private final WalletService walletService;
    private final Logger log = LoggerFactory.getLogger(LoanService.class);
    public LoanService(LoanRepository loanRepository,
                       RepaymentScheduleRepository scheduleRepository,
                       LoanAmortizationService amortizationService,
                       TransactionService transactionService,
                       WalletService walletService) {
        this.loanRepository = loanRepository;
        this.scheduleRepository = scheduleRepository;
        this.amortizationService = amortizationService;
        this.transactionService = transactionService;
        this.walletService = walletService;
    }

    public Loan apply(User user, BigDecimal loanAmount, Integer tenureMonths) {
        log.info("======== Initiating Loan application ===========");
        Loan loan = new Loan();
        loan.setUser(user);
        loan.setLoanAmount(loanAmount);
        loan.setInterestRate(DEFAULT_INTEREST_RATE);
        loan.setTenureMonths(tenureMonths);
        return loanRepository.save(loan);
    }

    /**
     * Transitions a PENDING loan to APPROVED and persists the full repayment
     * schedule in one atomic transaction. If schedule generation or any save
     * fails the entire operation rolls back — no partial schedules, no orphaned
     * status changes.
     */
    public Loan approveLoan(UUID loanId) {
        return approveLoan(loanId, DEFAULT_METHOD);
    }


    public Loan approveLoan(UUID loanId, AmortizationMethod method) {
        Loan loan = loanRepository.findByIdWithLock(loanId)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new BusinessRuleException(
                "Only PENDING loans can be approved — current status: " + loan.getStatus());
        }

        loan.setStatus(LoanStatus.APPROVED);

        List<RepaymentSchedule> schedule = amortizationService.generateSchedule(loan, method);
        scheduleRepository.saveAll(schedule);

        return loan;
    }

    public Loan disburseLoan(UUID loanId, String idempotencyKey) {
        Loan loan = loanRepository.findByIdWithLock(loanId)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (loan.getStatus() != LoanStatus.APPROVED) {
            throw new BusinessRuleException(
                "Only APPROVED loans can be disbursed — current status: " + loan.getStatus());
        }

        // Credit the borrower's wallet before updating the loan status.
        // If the wallet credit fails the transaction rolls back, keeping the loan APPROVED
        // and making the operation safely retryable with the same idempotency key.
        User borrower = loan.getUser();
        Wallet wallet = walletService.getWalletByUserId(borrower.getId());
        transactionService.credit(
            wallet.getId(),
            borrower,
            loan.getLoanAmount(),
            TransactionType.Disbursement,
            "Loan disbursement — loan " + loanId,
            idempotencyKey
        );

        loan.setStatus(LoanStatus.DISBURSED);
        return loan;
    }

    /**
     * Debits the borrower's wallet by the next due installment amount, marks that
     * installment PAID, and transitions the loan to REPAID once all installments
     * are settled. The pessimistic lock on the loan row serialises concurrent
     * repayment attempts so two calls can never both read the same UNPAID installment.
     */
    public LoanRepaymentResult repay(UUID loanId, BigDecimal amount, User user, String idempotencyKey) {
        Loan loan = loanRepository.findByIdWithLock(loanId)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));

        if (!loan.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Loan not found");
        }

        if (loan.getStatus() != LoanStatus.DISBURSED) {
            throw new BusinessRuleException(
                "Only DISBURSED loans can be repaid — current status: " + loan.getStatus());
        }

        RepaymentSchedule nextInstallment = scheduleRepository
            .findFirstByLoanIdAndStatusOrderByInstallmentNumberAsc(loanId, RepaymentScheduleStatus.UNPAID)
            .orElseThrow(() -> new BusinessRuleException("No outstanding installments found for this loan"));

        if (amount.compareTo(nextInstallment.getTotalInstallment()) != 0) {
            throw new BusinessRuleException(String.format(
                "Repayment amount must equal installment #%d total of %s",
                nextInstallment.getInstallmentNumber(), nextInstallment.getTotalInstallment()));
        }

        Wallet wallet = walletService.getWalletByUserId(user.getId());
        transactionService.debit(
            wallet.getId(), user, amount, TransactionType.Repayment,
            String.format("Repayment — loan %s installment #%d",
                loanId, nextInstallment.getInstallmentNumber()),
            idempotencyKey
        );

        nextInstallment.setStatus(RepaymentScheduleStatus.PAID);

        long remainingUnpaid = scheduleRepository.countByLoanIdAndStatus(loanId, RepaymentScheduleStatus.UNPAID);
        if (remainingUnpaid == 0) {
            loan.setStatus(LoanStatus.REPAID);
        }

        return new LoanRepaymentResult(loan, nextInstallment);
    }

    public record LoanRepaymentResult(Loan loan, RepaymentSchedule paidInstallment) {}

    @Transactional(readOnly = true)
    public List<Loan> getUserLoans(UUID userId) {
        return loanRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Loan getLoan(UUID loanId, User requestingUser) {
        Loan loan = loanRepository.findById(loanId)
            .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        if (!loan.getUser().getId().equals(requestingUser.getId())) {
            throw new ResourceNotFoundException("Loan not found");
        }
        return loan;
    }

    @Transactional(readOnly = true)
    public List<RepaymentSchedule> getSchedule(UUID loanId) {
        return scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId);
    }
}
