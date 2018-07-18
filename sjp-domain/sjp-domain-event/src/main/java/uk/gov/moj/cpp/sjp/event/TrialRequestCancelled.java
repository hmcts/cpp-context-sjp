package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.trial-request-cancelled")
public class TrialRequestCancelled {
    private final UUID caseId;

    @JsonCreator
    public TrialRequestCancelled(@JsonProperty("caseId") final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
