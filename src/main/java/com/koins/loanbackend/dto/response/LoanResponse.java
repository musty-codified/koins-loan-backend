package com.koins.loanbackend.dto.response;

import com.koins.loanbackend.domain.Loan;
import com.koins.loanbackend.domain.enums.LoanStatus;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class LoanResponse {

    private UUID id;
    private UUID userId;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer tenureMonths;
    private LoanStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LoanResponse from(Loan loan) {
        LoanResponse r = new LoanResponse();
        r.id = loan.getId();
        r.userId = loan.getUser().getId();
        r.loanAmount = loan.getLoanAmount();
        r.interestRate = loan.getInterestRate();
        r.tenureMonths = loan.getTenureMonths();
        r.status = loan.getStatus();
        r.createdAt = loan.getCreatedAt();
        r.updatedAt = loan.getUpdatedAt();
        return r;
    }
}