package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(EnforcementPendingApplicationNotificationQueued.EVENT_NAME)
public class EnforcementPendingApplicationNotificationQueued {

    public static final String EVENT_NAME = "sjp.events.enforcement-pending-application-notification-queued";

    private final UUID applicationId;
    private final ZonedDateTime queuedTime;

    @JsonCreator
    public EnforcementPendingApplicationNotificationQueued(
            @JsonProperty("applicationId") final UUID applicationId,
            @JsonProperty("queuedTime") final ZonedDateTime queuedTime) {
        this.applicationId = applicationId;
        this.queuedTime = queuedTime;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public ZonedDateTime getQueuedTime() {
        return queuedTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
