package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(NotificationToRemoveEndorsementsGenerationFailed.EVENT_NAME)
public class NotificationToRemoveEndorsementsGenerationFailed {

    public static final String EVENT_NAME = "sjp.events.notification-remove-endorsements-generation-failed";

    private final UUID applicationDecisionId;

    @JsonCreator
    public NotificationToRemoveEndorsementsGenerationFailed(
            @JsonProperty("applicationDecisionId") final UUID applicationDecisionId) {
        this.applicationDecisionId = applicationDecisionId;
    }

    public UUID getApplicationDecisionId() {
        return applicationDecisionId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
