package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.time.LocalDate;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Event for case reopened undone
 */
@Event("structure.events.case-reopened-in-libra-undone")
public class CaseReopenedUndone implements Serializable {

    private static final long serialVersionUID = 4206671253527856605L;

    private final String caseId;

    private final LocalDate oldReopenedDate;

    public CaseReopenedUndone(final String caseId, final LocalDate oldReopenedDate) {
        this.caseId = caseId;
        this.oldReopenedDate = oldReopenedDate;
    }

    public String getCaseId() {
        return caseId;
    }

    public LocalDate getOldReopenedDate() {
        return oldReopenedDate;
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
