package com.koins.loanbackend.service;

import com.koins.loanbackend.domain.RepaymentSchedule;
import com.koins.loanbackend.domain.enums.LoanStatus;
import com.koins.loanbackend.domain.enums.RepaymentScheduleStatus;
import com.koins.loanbackend.repository.LoanRepository;
import com.koins.loanbackend.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class LoanScheduler {

    @Value("${app.loan.late-fee-rate:0.02}")
    private BigDecimal lateFeeRate;

    @Value("${app.loan.default-threshold:3}")
    private int defaultThreshold;

    private final RepaymentScheduleRepository scheduleRepository;
    private final LoanRepository loanRepository;

    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void processOverdueInstallments() {
        LocalDate today = LocalDate.now();

        List<RepaymentSchedule> newlyOverdue =
            scheduleRepository.findByDueDateBeforeAndStatus(today, RepaymentScheduleStatus.UNPAID);

        if (newlyOverdue.isEmpty()) {
            log.debug("Overdue check complete — no newly overdue installments");
            return;
        }

        Set<UUID> affectedLoanIds = new HashSet<>();

        for (RepaymentSchedule installment : newlyOverdue) {
            BigDecimal lateFee = installment.getTotalInstallment()
                .multiply(lateFeeRate)
                .setScale(2, RoundingMode.HALF_UP);

            installment.setLateFee(lateFee);
            installment.setStatus(RepaymentScheduleStatus.OVERDUE);
            affectedLoanIds.add(installment.getLoan().getId());
        }
        log.info("Marked {} installment(s) OVERDUE across {} loan(s)",
            newlyOverdue.size(), affectedLoanIds.size());

        flagDefaultedLoans(affectedLoanIds);
    }

    private void flagDefaultedLoans(Set<UUID> loanIds) {
        for (UUID loanId : loanIds) {
            long overdueCount =
                scheduleRepository.countByLoanIdAndStatus(loanId, RepaymentScheduleStatus.OVERDUE);

            if (overdueCount < defaultThreshold) continue;

            loanRepository.findByIdWithLock(loanId).ifPresent(loan -> {
                if (loan.getStatus() == LoanStatus.DISBURSED) {
                    loan.setStatus(LoanStatus.DEFAULTED);
                    log.warn("Loan [{}] DEFAULTED — {} overdue installment(s) (threshold: {})",
                        loanId, overdueCount, defaultThreshold);
                }
            });
        }
    }
}