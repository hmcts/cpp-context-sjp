package uk.gov.moj.cpp.sjp.event;


import static uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAddedBdf.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
public class FinancialImpositionAccountNumberAddedBdf {

    public static final String EVENT_NAME = "sjp.events.financial-imposition-account-number-added-bdf";

    private final UUID caseId;

    private final UUID defendantId;

    private final UUID correlationId;

    private final String accountNumber;

    @JsonCreator
    public FinancialImpositionAccountNumberAddedBdf(@JsonProperty("caseId") final UUID caseId,
                                                    @JsonProperty("defendantId") final UUID defendantId,
                                                    @JsonProperty("correlationId") final UUID correlationId,
                                                    @JsonProperty("accountNumber") final String accountNumber) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.correlationId = correlationId;
        this.accountNumber = accountNumber;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCorrelationId() { return correlationId; }

    public String getAccountNumber() {
        return accountNumber;
    }
}
