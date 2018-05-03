package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Event(CaseAlreadyAssigned.EVENT_NAME)
public class CaseAlreadyAssigned {

    public static final String EVENT_NAME = "sjp.events.case-already-assigned";

    private final UUID caseId;
    private final UUID assigneeId;

    @JsonCreator
    public CaseAlreadyAssigned(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("assigneeId") final UUID assigneeId) {
        this.caseId = caseId;
        this.assigneeId = assigneeId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
