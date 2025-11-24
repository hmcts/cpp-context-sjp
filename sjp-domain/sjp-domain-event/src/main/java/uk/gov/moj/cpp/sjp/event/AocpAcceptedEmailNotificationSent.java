package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.apache.commons.lang3.builder.ToStringBuilder;
import uk.gov.justice.domain.annotation.Event;

@Event(AocpAcceptedEmailNotificationSent.EVENT_NAME)
public class AocpAcceptedEmailNotificationSent {

    public static final String EVENT_NAME = "sjp.events.aocp-accepted-email-notification-sent";

    private final UUID caseId;
    private final ZonedDateTime sentTime;

    @JsonCreator
    public AocpAcceptedEmailNotificationSent(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("sentTime") final ZonedDateTime sentTime) {
        this.caseId = caseId;
        this.sentTime = sentTime;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public ZonedDateTime getSentTime() {
        return sentTime;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
