package uk.gov.moj.cpp.sjp.event;


import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("sjp.events.case-not-found")
public class CaseNotFound {

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
