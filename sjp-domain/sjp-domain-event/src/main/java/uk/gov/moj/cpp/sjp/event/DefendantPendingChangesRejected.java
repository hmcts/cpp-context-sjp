package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Issued when LA rejects the changes for given defendant and case.
 */
@Event("sjp.events.defendant-pending-changes-rejected")
public class DefendantPendingChangesRejected {
    private final UUID caseId;
    private final UUID defendantId;
    private final String description;
    private final ZonedDateTime rejectedAt;

    @JsonCreator
    public DefendantPendingChangesRejected(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("defendantId") final UUID defendantId,
            @JsonProperty("description") final String description,
            @JsonProperty("rejectedAt") final ZonedDateTime rejectedAt) {

        this.caseId = caseId;
        this.defendantId = defendantId;
        this.description = description;
        this.rejectedAt = rejectedAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getDescription() {
        return description;
    }

    public ZonedDateTime getRejectedAt() {
        return rejectedAt;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
