package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Issued when LA accepts the changes for given defendant and case.
 */
@Event("sjp.events.defendant-pending-changes-accepted")
public class DefendantPendingChangesAccepted {
    private final UUID caseId;
    private final UUID defendantId;
    private final ZonedDateTime acceptedAt;

    @JsonCreator
    public DefendantPendingChangesAccepted(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("defendantId") final UUID defendantId,
            @JsonProperty("acceptedAt") final ZonedDateTime acceptedAt) {

        this.caseId = caseId;
        this.defendantId = defendantId;
        this.acceptedAt = acceptedAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public ZonedDateTime getAcceptedAt() {
        return acceptedAt;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
    }

    public static class DefendantPendingChangesAcceptedBuilder {
        private UUID caseId;
        private UUID defendantId;
        private ZonedDateTime acceptedAt;

        public static DefendantPendingChangesAcceptedBuilder defendantPendingChangesAccepted() {
            return new DefendantPendingChangesAcceptedBuilder();
        }

        public DefendantPendingChangesAcceptedBuilder withCaseId(UUID caseId) {
            this.caseId = caseId;
            return this;
        }

        public DefendantPendingChangesAcceptedBuilder withDefendantId(UUID defendantId) {
            this.defendantId = defendantId;
            return this;
        }

        public DefendantPendingChangesAcceptedBuilder withAcceptedAt(ZonedDateTime acceptedAt) {
            this.acceptedAt = acceptedAt;
            return this;
        }

        public DefendantPendingChangesAccepted build() {
            return new DefendantPendingChangesAccepted(caseId, defendantId, acceptedAt);
        }
    }
}
