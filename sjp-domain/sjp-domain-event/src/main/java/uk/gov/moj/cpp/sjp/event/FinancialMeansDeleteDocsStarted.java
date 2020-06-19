package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(FinancialMeansDeleteDocsStarted.EVENT_NAME)
public class FinancialMeansDeleteDocsStarted {

    public static final String EVENT_NAME = "sjp.events.financial-means-delete-docs-started";

    private final UUID caseId;

    private final UUID defendantId;


    @JsonCreator
    public FinancialMeansDeleteDocsStarted(@JsonProperty("caseId") final UUID caseId,
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
}
