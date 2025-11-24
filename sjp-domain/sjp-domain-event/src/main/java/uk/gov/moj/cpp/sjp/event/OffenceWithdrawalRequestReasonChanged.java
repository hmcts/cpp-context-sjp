package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Event(OffenceWithdrawalRequestReasonChanged.EVENT_NAME)
public class OffenceWithdrawalRequestReasonChanged {

    public static final String EVENT_NAME = "sjp.events.offence-withdrawal-request-reason-changed";

    private final UUID caseId;

    private final UUID offenceId;

    private final UUID changedBy;

    private final ZonedDateTime changedAt;

    private final UUID newWithdrawalRequestReasonId;

    private final UUID oldWithdrawalRequestReasonId;

    @JsonCreator
    public OffenceWithdrawalRequestReasonChanged(@JsonProperty("caseId") final UUID caseId,
                                                 @JsonProperty("offenceId") final UUID offenceId,
                                                 @JsonProperty("changedBy") final UUID changedBy,
                                                 @JsonProperty("changedAt") final ZonedDateTime changedAt,
                                                 @JsonProperty("newWithdrawalRequestReasonId") final UUID newWithdrawalRequestReasonId,
                                                 @JsonProperty("oldWithdrawalRequestReasonId") final UUID oldWithdrawalRequestReasonId) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.changedBy = changedBy;
        this.changedAt = changedAt;
        this.newWithdrawalRequestReasonId = newWithdrawalRequestReasonId;
        this.oldWithdrawalRequestReasonId = oldWithdrawalRequestReasonId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public UUID getChangedBy() {
        return changedBy;
    }

    public ZonedDateTime getChangedAt() {
        return changedAt;
    }

    public UUID getNewWithdrawalRequestReasonId() {
        return newWithdrawalRequestReasonId;
    }

    public UUID getOldWithdrawalRequestReasonId() {
        return oldWithdrawalRequestReasonId;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
