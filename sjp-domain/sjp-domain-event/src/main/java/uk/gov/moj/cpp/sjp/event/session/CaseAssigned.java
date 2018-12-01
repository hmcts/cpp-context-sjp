package uk.gov.moj.cpp.sjp.event.session;

import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Event(CaseAssigned.EVENT_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CaseAssigned {

    public static final String EVENT_NAME = "sjp.events.case-assigned";

    private final UUID caseId;
    private final UUID assigneeId;
    private final ZonedDateTime assignedAt;
    //TODO remove (ATCM-3097)
    private final CaseAssignmentType caseAssignmentType;

    @JsonCreator
    public CaseAssigned(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("assigneeId") final UUID assigneeId,
            @JsonProperty("assignedAt") final ZonedDateTime assignedAt,
            @JsonProperty("caseAssignmentType") final CaseAssignmentType caseAssignmentType) {
        this.caseId = caseId;
        this.assigneeId = assigneeId;
        this.assignedAt = assignedAt;
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

    public ZonedDateTime getAssignedAt() {
        return assignedAt;
    }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, SHORT_PREFIX_STYLE);
    }

}
