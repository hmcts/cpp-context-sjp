package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.commons.lang3.builder.ToStringBuilder;
import uk.gov.justice.domain.annotation.Event;

@Event(AocpAcceptedEmailNotificationFailed.EVENT_NAME)
public class AocpAcceptedEmailNotificationFailed {

    public static final String EVENT_NAME = "sjp.events.aocp-accepted-email-notification-failed";

    private final UUID caseId;
    private final ZonedDateTime failedTime;

    @JsonCreator
    public AocpAcceptedEmailNotificationFailed(
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
