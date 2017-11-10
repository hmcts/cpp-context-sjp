package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

@Event("structure.events.defendant-not-found")
public class DefendantNotFound {

    private final String defendantId;
    private final String description;

    public DefendantNotFound(String defendantId, String description) {
        this.defendantId = defendantId;
        this.description = description;
    }

    public String getDefendantId() {
        return defendantId;
    }

    public String getDescription() {
        return description;
    }
}
