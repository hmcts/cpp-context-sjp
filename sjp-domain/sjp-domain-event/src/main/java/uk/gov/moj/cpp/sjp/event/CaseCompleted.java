package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(CaseCompleted.EVENT_NAME)
public class CaseCompleted {

    public static final String EVENT_NAME = "sjp.events.case-completed";

    private final UUID caseId;

    @JsonCreator
    public CaseCompleted(@JsonProperty("caseId") UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }

}
