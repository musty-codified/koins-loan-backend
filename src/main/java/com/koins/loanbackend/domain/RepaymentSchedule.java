package com.koins.loanbackend.domain;

import com.koins.loanbackend.domain.enums.RepaymentScheduleStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "repayment_schedules", indexes = {
    @Index(name = "idx_rs_loan_id",   columnList = "loan_id"),
    @Index(name = "idx_rs_due_date",  columnList = "due_date"),
    @Index(name = "idx_rs_status",    columnList = "status")
})
public class RepaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "interest_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal interestAmount;

    @Column(name = "total_installment", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalInstallment;

    @Column(name = "late_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal lateFee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RepaymentScheduleStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void prePersist() {
        createdAt = LocalDateTime.now();
        if (status == null)  status  = RepaymentScheduleStatus.UNPAID;
        if (lateFee == null) lateFee = BigDecimal.ZERO.setScale(2);
    }

    public UUID getId() { return id; }

    public Loan getLoan() { return loan; }
    public void setLoan(Loan loan) { this.loan = loan; }

    public Integer getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(Integer installmentNumber) { this.installmentNumber = installmentNumber; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }

    public BigDecimal getInterestAmount() { return interestAmount; }
    public void setInterestAmount(BigDecimal interestAmount) { this.interestAmount = interestAmount; }

    public BigDecimal getTotalInstallment() { return totalInstallment; }
    public void setTotalInstallment(BigDecimal totalInstallment) { this.totalInstallment = totalInstallment; }

    public BigDecimal getLateFee() { return lateFee; }
    public void setLateFee(BigDecimal lateFee) { this.lateFee = lateFee; }

    public RepaymentScheduleStatus getStatus() { return status; }
    public void setStatus(RepaymentScheduleStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}