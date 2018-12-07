package uk.gov.moj.cpp.sjp.event.decommissioned;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.case-assignment-deleted")
public class CaseAssignmentDeleted implements Serializable {

    private static final long serialVersionUID = 2L;

    final UUID caseId;
    final CaseAssignmentType caseAssignmentType;

    @JsonCreator
    public CaseAssignmentDeleted(@JsonProperty("caseId") final UUID caseId, @JsonProperty("caseAssignmentType") final CaseAssignmentType caseAssignmentType) {
        this.caseId = caseId;
        this.caseAssignmentType = caseAssignmentType;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public CaseAssignmentType getCaseAssignmentType() {
        return caseAssignmentType;
    }

}
