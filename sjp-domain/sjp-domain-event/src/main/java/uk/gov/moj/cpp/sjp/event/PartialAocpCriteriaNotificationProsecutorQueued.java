package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotificationProsecutorQueued.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(EVENT_NAME)
public class PartialAocpCriteriaNotificationProsecutorQueued {

    public static final String EVENT_NAME = "sjp.events.partial-aocp-criteria-notification-to-prosecutor-queued";

    private final UUID caseId;

    @JsonCreator
    public PartialAocpCriteriaNotificationProsecutorQueued(
            @JsonProperty("caseId") final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
