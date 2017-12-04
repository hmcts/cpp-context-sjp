package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseAssignment;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Event("sjp.events.case-assignment-created")
public class CaseAssignmentCreated implements Serializable {

    private static final long serialVersionUID = -5037834839051764611L;

    @JsonUnwrapped
    private final CaseAssignment caseAssignment;

    public CaseAssignmentCreated(final CaseAssignment caseAssignment) {
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
