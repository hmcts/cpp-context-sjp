package uk.gov.moj.cpp.sjp.event;

import static uk.gov.moj.cpp.sjp.event.FinancialMeansDeleteDocsRequestRejected.EVENT_NAME;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EVENT_NAME)
public class FinancialMeansDeleteDocsRequestRejected {

    public static final String EVENT_NAME = "sjp.events.financial-means-delete-docs-request-rejected";

    private final UUID caseId;

    @JsonCreator
    public FinancialMeansDeleteDocsRequestRejected(@JsonProperty("caseId") final UUID caseId) {
        this.caseId = caseId;
    }

    public UUID getCaseId() {
        return caseId;
    }
}
