package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(EnforcementPendingApplicationNotificationGenerated.EVENT_NAME)
public class EnforcementPendingApplicationNotificationGenerated {

    public static final String EVENT_NAME = "sjp.events.enforcement-pending-application-notification-generated";

    private final UUID applicationId;

    private final UUID fileId;

    private final ZonedDateTime generatedTime;

    @JsonCreator
    public EnforcementPendingApplicationNotificationGenerated(
            @JsonProperty("applicationId") final UUID applicationId,
            @JsonProperty("fileId") final UUID fileId,
            @JsonProperty("generatedTime") final ZonedDateTime generatedTime) {
        this.applicationId = applicationId;
        this.fileId = fileId;
        this.generatedTime = generatedTime;
    }

    public UUID getFileId() {
        return fileId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public ZonedDateTime getGeneratedTime() {
        return generatedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnforcementPendingApplicationNotificationGenerated)) {
            return false;
        }
        final EnforcementPendingApplicationNotificationGenerated that = (EnforcementPendingApplicationNotificationGenerated) o;
        final boolean equalsFileId = Objects.equals(getFileId(), that.getFileId());
        final boolean equalsGeneratedTimeAndFileId = Objects.equals(getGeneratedTime(), that.getGeneratedTime()) && equalsFileId;
        final boolean equalsAppId = Objects.equals(getApplicationId(), that.getApplicationId());
        return equalsAppId && equalsGeneratedTimeAndFileId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getApplicationId(), getFileId(), getGeneratedTime());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
