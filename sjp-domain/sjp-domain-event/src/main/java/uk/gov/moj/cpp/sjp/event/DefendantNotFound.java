package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("sjp.events.defendant-not-found")
public class DefendantNotFound {

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
