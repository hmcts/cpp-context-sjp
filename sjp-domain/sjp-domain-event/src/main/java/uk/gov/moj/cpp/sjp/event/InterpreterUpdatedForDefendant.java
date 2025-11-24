package uk.gov.moj.cpp.sjp.event;



import static uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Interpreter;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterpreterUpdatedForDefendant {

    public static final String EVENT_NAME = "sjp.events.interpreter-for-defendant-updated";

    private final UUID caseId;
    private final UUID defendantId;
    private final Interpreter interpreter;
    private final boolean updatedByOnlinePlea;
    private final ZonedDateTime updatedDate;

    @JsonCreator
    private InterpreterUpdatedForDefendant(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("defendantId") final UUID defendantId,
            @JsonProperty("interpreter") final Interpreter interpreter,
            @JsonProperty("updatedByOnlinePlea") final boolean updatedByOnlinePlea,
            @JsonProperty("updatedDate") final ZonedDateTime updatedDate) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.interpreter = interpreter;
        this.updatedByOnlinePlea = updatedByOnlinePlea;
        this.updatedDate = updatedDate;
    }

    public static InterpreterUpdatedForDefendant createEvent(final UUID caseId, final UUID defendantId, final String interpreterLanguage) {
        return new InterpreterUpdatedForDefendant(caseId, defendantId, Interpreter.of(interpreterLanguage), false, null);
    }

    public static InterpreterUpdatedForDefendant createEventForOnlinePlea(final UUID caseId, final UUID defendantId, final String interpreterLanguage, final ZonedDateTime updatedDate) {
        return new InterpreterUpdatedForDefendant(caseId, defendantId, Interpreter.of(interpreterLanguage), true, updatedDate);
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public boolean isUpdatedByOnlinePlea() {
        return updatedByOnlinePlea;
    }

    public ZonedDateTime getUpdatedDate() {
        return updatedDate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final InterpreterUpdatedForDefendant that = (InterpreterUpdatedForDefendant) o;
        return updatedByOnlinePlea == that.updatedByOnlinePlea &&
                caseId.equals(that.caseId) &&
                defendantId.equals(that.defendantId) &&
                interpreter.equals(that.interpreter);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, defendantId, interpreter, updatedByOnlinePlea);
    }
}
