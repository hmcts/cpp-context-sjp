package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event(PleaCancelled.EVENT_NAME)
public class PleaCancelled {

    public static final String EVENT_NAME = "sjp.events.plea-cancelled";

    private final UUID caseId;

    private final UUID offenceId;

    private final UUID defendantId;


    @JsonCreator
    public PleaCancelled(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("offenceId") final UUID offenceId,
            @JsonProperty("defendantId") final UUID defendantId) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.defendantId = defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }
}
