package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.util.UUID;

public class OffenceDecisionId implements Serializable {
    private UUID caseDecisionId;
    private UUID offenceId;

    public OffenceDecisionId(final UUID caseDecisionId, final UUID offenceId) {
        this.caseDecisionId = caseDecisionId;
        this.offenceId = offenceId;
    }

    public OffenceDecisionId() {
    }

    public UUID getCaseDecisionId() {
        return caseDecisionId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }
}
