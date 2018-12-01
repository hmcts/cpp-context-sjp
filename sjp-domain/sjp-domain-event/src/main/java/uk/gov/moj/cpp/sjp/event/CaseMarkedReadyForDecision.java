package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event(CaseMarkedReadyForDecision.EVENT_NAME)
public class CaseMarkedReadyForDecision {

    public static final String EVENT_NAME = "sjp.events.case-marked-ready-for-decision";

    private final UUID caseId;
    private final CaseReadinessReason reason;
    private final ZonedDateTime markedAt;

    @JsonCreator
    public CaseMarkedReadyForDecision(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("reason") final CaseReadinessReason reason,
            @JsonProperty("markedAt") final ZonedDateTime markedAt) {
        this.caseId = caseId;
        this.reason = reason;
        this.markedAt = markedAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public CaseReadinessReason getReason() {
        return reason;
    }

    public ZonedDateTime getMarkedAt() {
        return markedAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof CaseMarkedReadyForDecision)) {
            return false;
        }

        final CaseMarkedReadyForDecision that = (CaseMarkedReadyForDecision) o;
        return Objects.equals(caseId, that.caseId) &&
                reason == that.reason &&
                Objects.equals(markedAt, that.markedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, reason, markedAt);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
