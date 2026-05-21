package com.koins.loanbackend.dto.request;

import com.koins.loanbackend.domain.enums.AmortizationMethod;

public class LoanApproveRequest {

    private AmortizationMethod amortizationMethod = AmortizationMethod.REDUCING_BALANCE;

    public AmortizationMethod getAmortizationMethod() { return amortizationMethod; }
    public void setAmortizationMethod(AmortizationMethod amortizationMethod) {
        this.amortizationMethod = amortizationMethod;
    }
}