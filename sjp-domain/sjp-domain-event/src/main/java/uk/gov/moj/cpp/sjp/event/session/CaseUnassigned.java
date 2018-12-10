package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(CaseUnassigned.EVENT_NAME)
public class CaseUnassigned {

    public static final String EVENT_NAME = "sjp.events.case-unassigned";

    private final UUID caseId;

    @JsonCreator
    public CaseUnassigned(@JsonProperty("caseId") final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }

}
