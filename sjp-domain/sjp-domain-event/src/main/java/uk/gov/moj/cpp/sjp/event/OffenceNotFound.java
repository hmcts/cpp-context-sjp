package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("sjp.events.plea-update-failed")
public class OffenceNotFound {

    private final UUID offenceId;
    private final String description;

    public OffenceNotFound(final UUID offenceId, final String description) {
        this.offenceId = offenceId;
        this.description = description;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public String getDescription() {
        return description;
    }
}
