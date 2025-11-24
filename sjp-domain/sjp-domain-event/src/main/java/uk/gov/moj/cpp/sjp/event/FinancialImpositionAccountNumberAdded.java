package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAdded.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
public class FinancialImpositionAccountNumberAdded {

    public static final String EVENT_NAME = "sjp.events.financial-imposition-account-number-added";

    private final UUID caseId;

    private final UUID defendantId;

    private final String accountNumber;

    @JsonCreator
    public FinancialImpositionAccountNumberAdded(@JsonProperty("caseId") final UUID caseId,
                                                 @JsonProperty("defendantId") final UUID defendantId,
                                                 @JsonProperty("accountNumber") final String accountNumber) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.accountNumber = accountNumber;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}
