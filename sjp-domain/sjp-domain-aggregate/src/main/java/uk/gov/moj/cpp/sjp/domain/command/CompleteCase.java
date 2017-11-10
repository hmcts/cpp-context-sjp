package uk.gov.moj.cpp.sjp.domain.command;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.io.Serializable;
import java.util.UUID;

public class CompleteCase implements Serializable {

    private UUID caseId;

    public CompleteCase() {
        //required for jackson serialization
    }

    @JsonCreator
    public CompleteCase(String caseId) {
        this.caseId = UUID.fromString(caseId);
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }
}
