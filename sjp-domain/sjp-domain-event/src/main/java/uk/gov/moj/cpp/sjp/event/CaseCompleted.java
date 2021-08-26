package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

@Event(CaseCompleted.EVENT_NAME)
public class CaseCompleted {

    public static final String EVENT_NAME = "sjp.events.case-completed";

    private final UUID caseId;

    private Set<UUID> sessionIds;

    @JsonCreator
    public CaseCompleted(@JsonProperty("caseId") UUID caseId, @JsonProperty("sessionIds") Set<UUID> sessionIds) {
        this.caseId = caseId;
        this.sessionIds = sessionIds;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Set<UUID> getSessionIds() {
        return sessionIds;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
