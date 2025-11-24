package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(NotificationToRemoveEndorsementsGenerated.EVENT_NAME)
public class NotificationToRemoveEndorsementsGenerated {

    public static final String EVENT_NAME = "sjp.events.notification-remove-endorsements-generated";

    private final UUID applicationDecisionId;
    private final UUID fileId;

    @JsonCreator
    public NotificationToRemoveEndorsementsGenerated(
            @JsonProperty("applicationDecisionId") final UUID applicationDecisionId,
            @JsonProperty("fileId") final UUID fileId) {
        this.applicationDecisionId = applicationDecisionId;
        this.fileId = fileId;
    }

    public UUID getApplicationDecisionId() {
        return applicationDecisionId;
    }

    public UUID getFileId() {
        return fileId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
