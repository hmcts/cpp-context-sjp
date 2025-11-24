package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.DefendantResponseTimerExpired.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
public class DefendantResponseTimerExpired {
    public static final String EVENT_NAME = "sjp.events.defendant-response-timer-expired";

    private final UUID caseId;

    @JsonCreator
    public DefendantResponseTimerExpired(final @JsonProperty UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
