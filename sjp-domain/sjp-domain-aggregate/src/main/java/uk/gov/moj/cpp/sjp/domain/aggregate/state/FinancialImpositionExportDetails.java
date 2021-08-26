package uk.gov.moj.cpp.sjp.domain.aggregate.state;

import java.util.UUID;

public class FinancialImpositionExportDetails implements AggregateState {

    private UUID correlationId;

    private String accountNumber;

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(final UUID correlationId) {
        this.correlationId = correlationId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(final String accountNumber) {
        this.accountNumber = accountNumber;
    }
}
