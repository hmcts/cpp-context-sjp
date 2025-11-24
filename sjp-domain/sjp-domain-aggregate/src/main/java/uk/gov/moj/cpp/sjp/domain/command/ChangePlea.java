package uk.gov.moj.cpp.sjp.domain.command;


import java.util.UUID;

public abstract class ChangePlea {

    private final UUID caseId;
    private final UUID offenceId;

    public ChangePlea(UUID caseId, UUID offenceId) {
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
