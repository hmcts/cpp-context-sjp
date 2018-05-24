package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event(DefendantNotFound.EVENT_NAME)
public class DefendantNotFound {

    public static final String EVENT_NAME = "sjp.events.defendant-not-found";

    private final UUID defendantId;
    private final String description;

    public DefendantNotFound(final UUID defendantId, final String description) {
        this.defendantId = defendantId;
        this.description = description;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getDescription() {
        return description;
    }
}
