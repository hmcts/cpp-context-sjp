package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.DatesToAvoidTimerExpired.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
public class DatesToAvoidTimerExpired {
    public static final String EVENT_NAME = "sjp.events.dates-to-avoid-expired";

    private final UUID caseId;

    @JsonCreator
    public DatesToAvoidTimerExpired(final @JsonProperty UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
