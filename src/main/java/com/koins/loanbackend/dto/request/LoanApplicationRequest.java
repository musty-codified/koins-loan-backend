package com.koins.loanbackend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class LoanApplicationRequest {

    @NotNull
    @Positive
    private BigDecimal loanAmount;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer tenureMonths;
}