package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.commons.lang3.builder.ToStringBuilder;
import uk.gov.justice.domain.annotation.Event;

@Event(AocpAcceptedEmailNotificationQueued.EVENT_NAME)
public class AocpAcceptedEmailNotificationQueued {

    public static final String EVENT_NAME = "sjp.events.aocp-accepted-email-notification-queued";

    private final UUID caseId;
    private final ZonedDateTime queuedTime;

    @JsonCreator
    public AocpAcceptedEmailNotificationQueued(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("queuedTime") final ZonedDateTime queuedTime) {
        this.caseId = caseId;
        this.queuedTime = queuedTime;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public ZonedDateTime getQueuedTime() {
        return queuedTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
