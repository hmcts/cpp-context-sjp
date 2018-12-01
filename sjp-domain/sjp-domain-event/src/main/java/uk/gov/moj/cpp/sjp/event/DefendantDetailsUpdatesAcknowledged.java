package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Issued when a prosecutor acknowledges the updates for given defendant and case.
 */
@Event(DefendantDetailsUpdatesAcknowledged.EVENT_NAME)
public class DefendantDetailsUpdatesAcknowledged {

    public static final String EVENT_NAME = "sjp.events.defendant-details-updates-acknowledged";

    private final UUID caseId;
    private final UUID defendantId;
    private final ZonedDateTime acknowledgedAt;

    @JsonCreator
    public DefendantDetailsUpdatesAcknowledged(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("defendantId") final UUID defendantId,
            @JsonProperty("acknowledgedAt") final ZonedDateTime acknowledgedAt) {

        this.caseId = caseId;
        this.defendantId = defendantId;
        this.acknowledgedAt = acknowledgedAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public ZonedDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DefendantDetailsUpdatesAcknowledged)) {
            return false;
        }
        final DefendantDetailsUpdatesAcknowledged that = (DefendantDetailsUpdatesAcknowledged) o;

        return Objects.equals(caseId, that.caseId) &&
                Objects.equals(defendantId, that.defendantId) &&
                Objects.equals(acknowledgedAt, that.acknowledgedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, defendantId, acknowledgedAt);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
