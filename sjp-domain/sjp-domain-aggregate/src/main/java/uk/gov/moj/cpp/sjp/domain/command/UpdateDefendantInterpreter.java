package uk.gov.moj.cpp.sjp.domain.command;

import uk.gov.moj.cpp.sjp.domain.Interpreter;

import java.util.UUID;

public class UpdateDefendantInterpreter {

    private final UUID caseId;
    private final UUID defendantId;
    private final Interpreter interpreter;

    public UpdateDefendantInterpreter(UUID caseId, UUID defendantId, Interpreter interpreter) {
        super();
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.interpreter = interpreter;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

}
