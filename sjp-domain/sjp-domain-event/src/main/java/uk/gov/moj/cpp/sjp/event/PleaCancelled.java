package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(PleaCancelled.EVENT_NAME)
public class PleaCancelled {

    public static final String EVENT_NAME = "sjp.events.plea-cancelled";

    private final UUID caseId;
    private final UUID offenceId;

    @JsonCreator
    public PleaCancelled(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("offenceId") final UUID offenceId) {
        this.caseId = caseId;
        this.offenceId = offenceId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }
}
