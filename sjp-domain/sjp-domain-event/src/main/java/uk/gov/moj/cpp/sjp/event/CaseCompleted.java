package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event(CaseCompleted.EVENT_NAME)
public class CaseCompleted {

    public static final String EVENT_NAME = "sjp.events.case-completed";

    private final UUID caseId;

    @JsonCreator
    public CaseCompleted(@JsonProperty("caseId") UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CaseCompleted)) {
            return false;
        }
        final CaseCompleted that = (CaseCompleted) o;

        return Objects.equals(caseId, that.caseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
