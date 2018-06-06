package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(AllOffencesWithdrawalRequested.EVENT_NAME)
public class AllOffencesWithdrawalRequested {

    public static final String EVENT_NAME = "sjp.events.all-offences-withdrawal-requested";

    private UUID caseId;

    @JsonCreator
    public AllOffencesWithdrawalRequested(@JsonProperty("caseId") UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
