package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(EnforcementPendingApplicationNotificationFailed.EVENT_NAME)
public class EnforcementPendingApplicationNotificationFailed {

    public static final String EVENT_NAME = "sjp.events.enforcement-pending-application-notification-failed";

    private final UUID applicationId;
    private final ZonedDateTime failedTime;

    @JsonCreator
    public EnforcementPendingApplicationNotificationFailed(
            @JsonProperty("applicationId") final UUID applicationId,
            @JsonProperty("failedTime") final ZonedDateTime failedTime) {
        this.applicationId = applicationId;
        this.failedTime = failedTime;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public ZonedDateTime getFailedTime() {
        return failedTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
