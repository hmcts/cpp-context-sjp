package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(EnforcementPendingApplicationNotificationGenerationFailed.EVENT_NAME)
public class EnforcementPendingApplicationNotificationGenerationFailed {

    public static final String EVENT_NAME = "sjp.events.enforcement-pending-application-notification-generation-failed";

    private final UUID applicationId;
    private final ZonedDateTime generationFailedTime;

    @JsonCreator
    public EnforcementPendingApplicationNotificationGenerationFailed(
            @JsonProperty("applicationId") final UUID applicationId,
            @JsonProperty("generationFailedTime") final ZonedDateTime generationFailedTime) {
        this.applicationId = applicationId;
        this.generationFailedTime = generationFailedTime;
    }
    public ZonedDateTime getGenerationFailedTime() {
        return generationFailedTime;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
