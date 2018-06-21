package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(CaseUnmarkedReadyForDecision.EVENT_NAME)
public class CaseUnmarkedReadyForDecision {

    public static final String EVENT_NAME = "sjp.events.case-unmarked-ready-for-decision";

    private final UUID caseId;

    @JsonCreator
    public CaseUnmarkedReadyForDecision(@JsonProperty("caseId") final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
