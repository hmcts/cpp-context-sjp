package uk.gov.moj.cpp.sjp.event.decision;


import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(DecisionSetAside.EVENT_NAME)
public class DecisionSetAside {

    public static final String EVENT_NAME = "sjp.events.decision-set-aside";

    private UUID decisionId;

    private UUID caseId;

    @JsonCreator
    public DecisionSetAside(@JsonProperty("decisionId") final UUID decisionId,
                         @JsonProperty("caseId") final UUID caseId) {
        this.decisionId = decisionId;
        this.caseId = caseId;
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public UUID getCaseId() {
        return caseId;
    }


}
