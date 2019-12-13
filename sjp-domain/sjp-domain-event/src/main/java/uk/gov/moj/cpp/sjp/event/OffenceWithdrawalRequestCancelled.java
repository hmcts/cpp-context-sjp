package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Event(OffenceWithdrawalRequestCancelled.EVENT_NAME)
public class OffenceWithdrawalRequestCancelled {

    public static final String EVENT_NAME = "sjp.events.offence-withdrawal-request-cancelled";

    private final UUID caseId;

    private final UUID offenceId;

    private final UUID cancelledBy;

    private final ZonedDateTime cancelledAt;

    @JsonCreator
    public OffenceWithdrawalRequestCancelled(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("offenceId") final UUID offenceId,
            @JsonProperty("cancelledBy") final UUID cancelledBy,
            @JsonProperty("cancelledAt") final ZonedDateTime cancelledAt) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.cancelledBy = cancelledBy;
        this.cancelledAt = cancelledAt;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public UUID getCancelledBy() {
        return cancelledBy;
    }

    public ZonedDateTime getCancelledAt() {
        return cancelledAt;
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
