package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(PartialAocpCriteriaNotificationProsecutorFailed.EVENT_NAME)
public class PartialAocpCriteriaNotificationProsecutorFailed {

    public static final String EVENT_NAME = "sjp.events.partial-aocp-criteria-notification-to-prosecutor-failed";

    private final UUID caseId;
    private final ZonedDateTime failedTime;

    @JsonCreator
    public PartialAocpCriteriaNotificationProsecutorFailed(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("failedTime") final ZonedDateTime failedTime) {
        this.caseId = caseId;
        this.failedTime = failedTime;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public ZonedDateTime getFailedTime() {
        return failedTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
