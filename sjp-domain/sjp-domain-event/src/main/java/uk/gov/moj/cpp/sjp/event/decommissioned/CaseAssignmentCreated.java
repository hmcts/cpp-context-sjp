package uk.gov.moj.cpp.sjp.event.decommissioned;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Event("sjp.events.case-assignment-created")
public final class CaseAssignmentCreated implements Serializable {

    private static final long serialVersionUID = 2L;

    final UUID caseId;
    final CaseAssignmentType caseAssignmentType;

    @JsonCreator
    public CaseAssignmentCreated(@JsonProperty("caseId") final UUID caseId, @JsonProperty("caseAssignmentType") final CaseAssignmentType caseAssignmentType) {
        this.caseId = caseId;
        this.caseAssignmentType = caseAssignmentType;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public CaseAssignmentType getCaseAssignmentType() {
        return caseAssignmentType;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
