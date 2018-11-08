package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(PleaCancelled.EVENT_NAME)
public class PleaCancelled {

    public static final String EVENT_NAME = "sjp.events.plea-cancelled";

    private final UUID caseId;
    private final UUID offenceId;
    private final Boolean provedInAbsence;

    @JsonCreator
    public PleaCancelled(
            @JsonProperty("caseId") final UUID caseId,
            @JsonProperty("offenceId") final UUID offenceId,
            final Boolean provedInAbsence) {
        this.caseId = caseId;
        this.offenceId = offenceId;
        this.provedInAbsence = provedInAbsence;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public Boolean getProvedInAbsence() {
        return provedInAbsence;
    }
}
