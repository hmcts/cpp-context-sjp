package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.Interpreter;

import java.time.ZonedDateTime;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event("sjp.events.interpreter-for-defendant-updated")
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterpreterUpdatedForDefendant {

    private UUID caseId;
    private UUID defendantId;
    private Interpreter interpreter;
    private boolean updatedByOnlinePlea;
    private ZonedDateTime updatedDate;

    @JsonCreator
    private InterpreterUpdatedForDefendant(
            @JsonProperty("caseId") UUID caseId,
            @JsonProperty("defendantId") UUID defendantId,
            @JsonProperty("interpreter") Interpreter interpreter,
            @JsonProperty("updatedByOnlinePlea") boolean updatedByOnlinePlea,
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

}
