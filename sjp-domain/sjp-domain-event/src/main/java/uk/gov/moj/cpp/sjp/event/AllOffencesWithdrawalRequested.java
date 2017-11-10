package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("structure.events.all-offences-withdrawal-requested")
public class AllOffencesWithdrawalRequested {

    private UUID caseId;

    @JsonCreator
    public AllOffencesWithdrawalRequested(@JsonProperty("caseId") UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
