package com.koins.loanbackend.repository;

import com.koins.loanbackend.domain.RepaymentSchedule;
import com.koins.loanbackend.domain.enums.RepaymentScheduleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepaymentScheduleRepository extends JpaRepository<RepaymentSchedule, UUID> {

    List<RepaymentSchedule> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);

    List<RepaymentSchedule> findByLoanIdAndStatus(UUID loanId, RepaymentScheduleStatus status);

    Optional<RepaymentSchedule> findFirstByLoanIdAndStatusOrderByInstallmentNumberAsc(
        UUID loanId, RepaymentScheduleStatus status);

    Optional<RepaymentSchedule> findFirstByLoanIdAndStatusInOrderByInstallmentNumberAsc(
        UUID loanId, List<RepaymentScheduleStatus> statuses);

    long countByLoanIdAndStatus(UUID loanId, RepaymentScheduleStatus status);

    List<RepaymentSchedule> findByDueDateBeforeAndStatus(LocalDate date, RepaymentScheduleStatus status);
}