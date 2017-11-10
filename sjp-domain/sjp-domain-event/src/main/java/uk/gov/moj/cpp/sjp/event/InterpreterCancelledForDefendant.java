package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("structure.events.interpreter-for-defendant-cancelled")
public class InterpreterCancelledForDefendant {

    private UUID caseId;
    private UUID defendantId;

    @JsonCreator
    public InterpreterCancelledForDefendant(
            @JsonProperty(value = "caseId") UUID caseId,
            @JsonProperty(value = "defendantId") UUID defendantId) {
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
