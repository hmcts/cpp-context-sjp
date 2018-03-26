package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;

import java.io.Serializable;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Event("sjp.events.case-assigned")
public class CaseAssigned implements Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID caseId;
    private final UUID assigneeId;
    private final UUID sessionId;
    private final CaseAssignmentType caseAssignmentType;

    @JsonCreator
    public CaseAssigned(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("sessionId") final UUID sessionId,
            @JsonProperty("assigneeId") final UUID assigneeId,
            @JsonProperty("caseAssignmentType") final CaseAssignmentType caseAssignmentType) {
        this.caseId = caseId;
        this.assigneeId = assigneeId;
        this.sessionId = sessionId;
        this.caseAssignmentType = caseAssignmentType;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public CaseAssignmentType getCaseAssignmentType() {
        return caseAssignmentType;
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
