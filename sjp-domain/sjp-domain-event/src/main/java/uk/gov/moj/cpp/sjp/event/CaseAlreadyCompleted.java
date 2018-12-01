package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event("sjp.events.case-completion-failed")
public class CaseAlreadyCompleted implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID caseId;
    private final String description;

    public CaseAlreadyCompleted(UUID caseId, String description) {
        this.caseId = caseId;
        this.description = description;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CaseAlreadyCompleted)) {
            return false;
        }
        final CaseAlreadyCompleted that = (CaseAlreadyCompleted) o;

        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, description);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
