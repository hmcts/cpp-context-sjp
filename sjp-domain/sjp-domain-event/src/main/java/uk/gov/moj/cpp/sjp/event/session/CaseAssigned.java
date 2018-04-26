package uk.gov.moj.cpp.sjp.event.session;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Event(CaseAssigned.EVENT_NAME)
public class CaseAssigned {

    public static final String EVENT_NAME = "sjp.events.case-assigned";

    private final UUID caseId;
    private final UUID assigneeId;
    private final CaseAssignmentType caseAssignmentType;

    @JsonCreator
    public CaseAssigned(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("assigneeId") final UUID assigneeId,
            @JsonProperty("caseAssignmentType") final CaseAssignmentType caseAssignmentType) {
        this.caseId = caseId;
        this.assigneeId = assigneeId;
        this.caseAssignmentType = caseAssignmentType;
    }

    public UUID getCaseId() {
        return caseId;
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
