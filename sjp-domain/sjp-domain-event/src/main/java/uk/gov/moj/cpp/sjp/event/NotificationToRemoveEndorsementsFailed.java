package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(NotificationToRemoveEndorsementsFailed.EVENT_NAME)
public class NotificationToRemoveEndorsementsFailed {

    public static final String EVENT_NAME = "sjp.events.notification-remove-endorsements-failed";

    private final UUID applicationDecisionId;
    private final ZonedDateTime failedTime;

    @JsonCreator
    public NotificationToRemoveEndorsementsFailed(
            @JsonProperty("applicationDecisionId") final UUID applicationDecisionId,
            @JsonProperty("failedTime") final ZonedDateTime failedTime) {
        this.applicationDecisionId = applicationDecisionId;
        this.failedTime = failedTime;
    }

    public UUID getApplicationDecisionId() {
        return applicationDecisionId;
    }

    public ZonedDateTime getFailedTime() {
        return failedTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
