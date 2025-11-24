package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
public class InterpreterCancelledForDefendant {

    public static final String EVENT_NAME = "sjp.events.interpreter-for-defendant-cancelled";

    private final UUID caseId;
    private final UUID defendantId;

    @JsonCreator
    public InterpreterCancelledForDefendant(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("defendantId") final UUID defendantId) {
        this.caseId = caseId;
        this.defendantId = defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final InterpreterCancelledForDefendant that = (InterpreterCancelledForDefendant) o;
        return caseId.equals(that.caseId) &&
                defendantId.equals(that.defendantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, defendantId);
    }
}
