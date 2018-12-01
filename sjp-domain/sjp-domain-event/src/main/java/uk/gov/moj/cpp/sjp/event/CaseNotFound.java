package uk.gov.moj.cpp.sjp.event;


import static uk.gov.moj.cpp.sjp.event.CaseNotFound.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event(EVENT_NAME)
public class CaseNotFound {

    public static final String EVENT_NAME = "sjp.events.case-not-found";

    private final UUID caseId;
    private final String description;

    public CaseNotFound(UUID caseId, String description) {
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
        if (!(o instanceof CaseNotFound)) {
            return false;
        }
        final CaseNotFound that = (CaseNotFound) o;

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
