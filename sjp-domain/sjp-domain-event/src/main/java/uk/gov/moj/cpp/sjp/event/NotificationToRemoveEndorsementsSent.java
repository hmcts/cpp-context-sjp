package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(NotificationToRemoveEndorsementsSent.EVENT_NAME)
public class NotificationToRemoveEndorsementsSent {

    public static final String EVENT_NAME = "sjp.events.notification-remove-endorsements-sent";

    private final UUID applicationDecisionId;
    private final ZonedDateTime sentTime;

    @JsonCreator
    public NotificationToRemoveEndorsementsSent(
            @JsonProperty("applicationDecisionId") final UUID applicationDecisionId,
            @JsonProperty("sentTime") final ZonedDateTime sentTime) {
        this.applicationDecisionId = applicationDecisionId;
        this.sentTime = sentTime;
    }

    public UUID getApplicationDecisionId() {
        return applicationDecisionId;
    }

    public ZonedDateTime getSentTime() {
        return sentTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
