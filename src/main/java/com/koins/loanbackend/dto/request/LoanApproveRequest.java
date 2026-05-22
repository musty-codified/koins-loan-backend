package com.koins.loanbackend.dto.request;

import com.koins.loanbackend.domain.enums.AmortizationMethod;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoanApproveRequest {

    private AmortizationMethod amortizationMethod = AmortizationMethod.REDUCING_BALANCE;
}