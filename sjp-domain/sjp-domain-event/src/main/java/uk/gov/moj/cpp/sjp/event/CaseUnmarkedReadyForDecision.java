package uk.gov.moj.cpp.sjp.event;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(CaseUnmarkedReadyForDecision.EVENT_NAME)
public class CaseUnmarkedReadyForDecision {

    public static final String EVENT_NAME = "sjp.events.case-unmarked-ready-for-decision";

    private final UUID caseId;

    @JsonCreator
    public CaseUnmarkedReadyForDecision(@JsonProperty("caseId") final UUID caseId) {
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
        if (!(o instanceof CaseUnmarkedReadyForDecision)) {
            return false;
        }

        final CaseUnmarkedReadyForDecision that = (CaseUnmarkedReadyForDecision) o;
        return Objects.equals(caseId, that.caseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
