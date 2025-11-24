package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(PartialAocpCriteriaNotificationProsecutorSent.EVENT_NAME)
public class PartialAocpCriteriaNotificationProsecutorSent {

    public static final String EVENT_NAME = "sjp.events.partial-aocp-criteria-notification-to-prosecutor-sent";

    private final UUID caseId;
    private final ZonedDateTime sentTime;

    @JsonCreator
    public PartialAocpCriteriaNotificationProsecutorSent(
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
