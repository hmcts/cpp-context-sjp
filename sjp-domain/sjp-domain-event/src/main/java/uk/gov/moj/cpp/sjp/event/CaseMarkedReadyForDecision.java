package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.Priority;
import uk.gov.moj.cpp.sjp.domain.SessionType;

import java.time.ZonedDateTime;
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
    private final SessionType sessionType;
    private final Priority priority;

    @JsonCreator
    public CaseMarkedReadyForDecision(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("reason") final CaseReadinessReason reason,
            @JsonProperty("markedAt") final ZonedDateTime markedAt, // don't override it when raising the same event again
            @JsonProperty("sessionType") final SessionType sessionType,
            @JsonProperty("priority") final Priority priority) {
        this.caseId = caseId;
        this.reason = reason;
        this.markedAt = markedAt;
        this.sessionType = sessionType;
        this.priority = priority;
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

    public SessionType getSessionType() {
        return sessionType;
    }

    public Priority getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
