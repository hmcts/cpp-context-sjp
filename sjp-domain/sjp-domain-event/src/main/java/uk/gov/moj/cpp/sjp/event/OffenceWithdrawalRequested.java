package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Event(OffenceWithdrawalRequested.EVENT_NAME)
public class OffenceWithdrawalRequested {

    public static final String EVENT_NAME = "sjp.events.offence-withdrawal-requested";

    private final UUID caseId;

    private final UUID offenceId;

    private final UUID withdrawalRequestReasonId;

    private final UUID requestedBy;

    private final ZonedDateTime requestedAt;

    @JsonCreator
    public OffenceWithdrawalRequested(@JsonProperty("caseId") final UUID caseId,
                                      @JsonProperty("offenceId") final UUID offenceId,
                                      @JsonProperty("withdrawalRequestReasonId") final UUID withdrawalRequestReasonId,
                                      @JsonProperty("requestedBy") final UUID requestedBy,
                                      @JsonProperty("requestedAt") final ZonedDateTime requestedAt) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.withdrawalRequestReasonId = withdrawalRequestReasonId;
        this.requestedBy = requestedBy;
        this.requestedAt = requestedAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public UUID getWithdrawalRequestReasonId() {
        return withdrawalRequestReasonId;
    }

    public UUID getRequestedBy() {
        return requestedBy;
    }

    public ZonedDateTime getRequestedAt() {
        return requestedAt;
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
