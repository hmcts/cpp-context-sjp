package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseAssignment;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Event("sjp.events.case-assignment-deleted")
public class CaseAssignmentDeleted implements Serializable {

    private static final long serialVersionUID = -473852992216788449L;

    @JsonUnwrapped
    private final CaseAssignment caseAssignment;

    public CaseAssignmentDeleted(final CaseAssignment caseAssignment) {
        this.caseAssignment = caseAssignment;
    }

    public CaseAssignment getCaseAssignment() {
        return caseAssignment;
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
