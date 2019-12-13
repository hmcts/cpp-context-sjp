package uk.gov.moj.cpp.sjp.query.view.exception;


import java.util.UUID;

public class WithdrawalReasonNotFoundException extends RuntimeException {

    private final UUID withdrawalReasonId;

    public WithdrawalReasonNotFoundException(final UUID withdrawalReasonId) {
        super(withdrawalReasonId.toString());
        this.withdrawalReasonId = withdrawalReasonId;
    }

    public UUID getWithdrawalReasonId() {
        return withdrawalReasonId;
    }
}
