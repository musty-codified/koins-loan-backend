package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.Loan;
import com.koins.loanbackend.domain.RepaymentSchedule;
import com.koins.loanbackend.domain.enums.AmortizationMethod;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class LoanAmortizationService {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public List<RepaymentSchedule> generateSchedule(Loan loan, AmortizationMethod method) {
        return switch (method) {
            case REDUCING_BALANCE -> reducingBalance(loan);
            case FLAT_RATE        -> flatRate(loan);
        };
    }

    /**
     * Reducing-balance (EMI) amortization.
     *
     * EMI = P × r × (1 + r)^n / ((1 + r)^n − 1)
     * where r = monthly rate, n = tenure in months.
     *
     * Each period: interest = outstanding × r, principal = EMI − interest.
     * The last installment absorbs any rounding residual so the schedule
     * zeroes the principal exactly.
     */
    private List<RepaymentSchedule> reducingBalance(Loan loan) {
        BigDecimal P = loan.getLoanAmount();
        int n = loan.getTenureMonths();
        BigDecimal r = loan.getInterestRate()
            .divide(BigDecimal.valueOf(12), 10, ROUNDING);

        BigDecimal emi = computeEmi(P, r, n);

        List<RepaymentSchedule> schedule = new ArrayList<>(n);
        BigDecimal outstanding = P;
        LocalDate start = LocalDate.now();

        for (int i = 1; i <= n; i++) {
            BigDecimal interest = outstanding.multiply(r).setScale(SCALE, ROUNDING);
            BigDecimal principal;
            BigDecimal total;

            if (i == n) {
                // Last installment: clear any residual from rounding
                principal = outstanding;
                total = principal.add(interest);
            } else {
                principal = emi.subtract(interest);
                total = emi;
                outstanding = outstanding.subtract(principal);
            }

            schedule.add(buildEntry(loan, i, start.plusMonths(i), principal, interest, total));
        }

        return schedule;
    }

    /**
     * Flat-rate amortization.
     *
     * Total interest = P × annualRate × (n / 12).
     * Equal monthly payment = (P + totalInterest) / n.
     * Principal and interest portions are constant each period.
     */
    private List<RepaymentSchedule> flatRate(Loan loan) {
        BigDecimal P = loan.getLoanAmount();
        int n = loan.getTenureMonths();

        BigDecimal totalInterest = P
            .multiply(loan.getInterestRate())
            .multiply(BigDecimal.valueOf(n))
            .divide(BigDecimal.valueOf(12), SCALE, ROUNDING);

        BigDecimal monthlyPrincipal = P.divide(BigDecimal.valueOf(n), SCALE, ROUNDING);
        BigDecimal monthlyInterest  = totalInterest.divide(BigDecimal.valueOf(n), SCALE, ROUNDING);
        BigDecimal monthlyTotal     = monthlyPrincipal.add(monthlyInterest);

        List<RepaymentSchedule> schedule = new ArrayList<>(n);
        LocalDate start = LocalDate.now();

        for (int i = 1; i <= n; i++) {
            schedule.add(buildEntry(loan, i, start.plusMonths(i),
                monthlyPrincipal, monthlyInterest, monthlyTotal));
        }

        return schedule;
    }

    // EMI = P × r × (1+r)^n / ((1+r)^n − 1)
    private BigDecimal computeEmi(BigDecimal P, BigDecimal r, int n) {
        BigDecimal onePlusR    = BigDecimal.ONE.add(r);
        BigDecimal onePlusRPowN = onePlusR.pow(n, new MathContext(20, ROUNDING));
        BigDecimal numerator   = P.multiply(r).multiply(onePlusRPowN);
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE);
        return numerator.divide(denominator, SCALE, ROUNDING);
    }

    private RepaymentSchedule buildEntry(Loan loan, int number, LocalDate dueDate,
                                         BigDecimal principal, BigDecimal interest,
                                         BigDecimal total) {
        RepaymentSchedule rs = new RepaymentSchedule();
        rs.setLoan(loan);
        rs.setInstallmentNumber(number);
        rs.setDueDate(dueDate);
        rs.setPrincipalAmount(principal);
        rs.setInterestAmount(interest);
        rs.setTotalInstallment(total);
        // status defaults to UNPAID via @PrePersist
        return rs;
    }
}