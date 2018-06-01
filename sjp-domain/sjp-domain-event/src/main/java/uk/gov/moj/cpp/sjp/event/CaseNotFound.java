package uk.gov.moj.cpp.sjp.event;


import static uk.gov.moj.cpp.sjp.event.CaseNotFound.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event(EVENT_NAME)
public class CaseNotFound {

    public static final String EVENT_NAME = "sjp.events.case-not-found";

    private final UUID caseId;
    private final String description;

    public CaseNotFound(UUID caseId, String description) {
        this.caseId = caseId;
        this.description = description;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getDescription() {
        return description;
    }
}
