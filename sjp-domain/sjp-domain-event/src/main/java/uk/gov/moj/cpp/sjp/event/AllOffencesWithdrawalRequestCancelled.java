package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(AllOffencesWithdrawalRequestCancelled.EVENT_NAME)
public class AllOffencesWithdrawalRequestCancelled {

    public static final String EVENT_NAME = "sjp.events.all-offences-withdrawal-request-cancelled";

    private UUID caseId;

    @JsonCreator
    public AllOffencesWithdrawalRequestCancelled(@JsonProperty("caseId") UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
