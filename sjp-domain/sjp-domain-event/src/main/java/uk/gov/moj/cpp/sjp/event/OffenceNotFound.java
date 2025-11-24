package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;

@Event(OffenceNotFound.EVENT_NAME)
public class OffenceNotFound {

    public static final String EVENT_NAME = "sjp.events.plea-update-failed";

    private final UUID offenceId;
    private final String description;

    @JsonCreator
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
