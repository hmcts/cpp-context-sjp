package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.VerdictCancelled.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
public class VerdictCancelled {

    public static final String EVENT_NAME = "sjp.events.offence-verdict-cancelled";

    private final UUID offenceId;

    @JsonCreator
    public VerdictCancelled(@JsonProperty("offenceId") final UUID offenceId) {
        this.offenceId = offenceId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VerdictCancelled{");
        sb.append("offenceId=").append(offenceId);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof VerdictCancelled)) {
            return false;
        }
        final VerdictCancelled that = (VerdictCancelled) o;
        return offenceId.equals(that.offenceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offenceId);
    }
}
