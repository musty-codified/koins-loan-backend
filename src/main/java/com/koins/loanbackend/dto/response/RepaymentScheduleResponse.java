package com.koins.loanbackend.dto.response;

import com.koins.loanbackend.domain.RepaymentSchedule;
import com.koins.loanbackend.domain.enums.RepaymentScheduleStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class RepaymentScheduleResponse {

    private UUID id;
    private UUID loanId;
    private Integer installmentNumber;
    private LocalDate dueDate;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal totalInstallment;
    private BigDecimal lateFee;
    private BigDecimal amountDue;
    private RepaymentScheduleStatus status;
    private LocalDateTime createdAt;

    public static RepaymentScheduleResponse from(RepaymentSchedule rs) {
        RepaymentScheduleResponse r = new RepaymentScheduleResponse();
        r.id = rs.getId();
        r.loanId = rs.getLoan().getId();
        r.installmentNumber = rs.getInstallmentNumber();
        r.dueDate = rs.getDueDate();
        r.principalAmount = rs.getPrincipalAmount();
        r.interestAmount = rs.getInterestAmount();
        r.totalInstallment = rs.getTotalInstallment();
        r.lateFee = rs.getLateFee();
        r.amountDue = rs.getTotalInstallment().add(rs.getLateFee());
        r.status = rs.getStatus();
        r.createdAt = rs.getCreatedAt();
        return r;
    }
}