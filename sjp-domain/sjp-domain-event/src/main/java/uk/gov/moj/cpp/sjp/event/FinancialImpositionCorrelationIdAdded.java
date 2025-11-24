package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.FinancialImpositionCorrelationIdAdded.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
public class FinancialImpositionCorrelationIdAdded {

    public static final String EVENT_NAME = "sjp.events.financial-imposition-correlation-id-added";

    private final UUID caseId;

    private final UUID defendantId;

    private final UUID correlationId;

    @JsonCreator
    public FinancialImpositionCorrelationIdAdded(@JsonProperty("caseId") final UUID caseId,
                                                 @JsonProperty("defendantId") final UUID defendantId,
                                                 @JsonProperty("correlationId") final UUID correlationId) {
        this.caseId = caseId;
        this.defendantId = defendantId;
        this.correlationId = correlationId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }
}
