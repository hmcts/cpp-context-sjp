package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.interpreter-for-defendant-cancelled")
public class InterpreterCancelledForDefendant {

    private UUID caseId;
    private UUID defendantId;

    @JsonCreator
    public InterpreterCancelledForDefendant(
            @JsonProperty("caseId") UUID caseId,
            @JsonProperty("defendantId") UUID defendantId) {
        this.caseId = caseId;
        this.defendantId = defendantId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

}
