package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(EnforcementPendingApplicationNotificationSent.EVENT_NAME)
public class EnforcementPendingApplicationNotificationSent {

    public static final String EVENT_NAME = "sjp.events.enforcement-pending-application-notification-sent";

    private final UUID applicationId;
    private final ZonedDateTime sentTime;

    @JsonCreator
    public EnforcementPendingApplicationNotificationSent(
            @JsonProperty("applicationId") final UUID applicationId,
            @JsonProperty("sentTime") final ZonedDateTime sentTime) {
        this.applicationId = applicationId;
        this.sentTime = sentTime;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public ZonedDateTime getSentTime() {
        return sentTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
