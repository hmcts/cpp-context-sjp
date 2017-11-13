package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Interpreter;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.interpreter-for-defendant-updated")
public class InterpreterUpdatedForDefendant {

    private UUID caseId;
    private UUID defendantId;
    private Interpreter interpreter;

    @JsonCreator
    public InterpreterUpdatedForDefendant(
            @JsonProperty(value = "caseId") UUID caseId,
            @JsonProperty(value = "defendantId") UUID defendantId,
            @JsonProperty(value = "interpreter") Interpreter interpreter) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.interpreter = interpreter;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

}
