package uk.gov.moj.cpp.sjp.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

import static uk.gov.moj.cpp.sjp.event.DeleteCaseDocumentRequestRejected.EVENT_NAME;

@Event(EVENT_NAME)
public class DeleteCaseDocumentRequestRejected {

    public static final String EVENT_NAME = "sjp.events.delete-case-document-request-rejected";

    private final UUID caseId;
    private final UUID documentId;
    private final String reason;

    @JsonCreator
    public DeleteCaseDocumentRequestRejected(@JsonProperty("caseId") final UUID caseId,
                                             @JsonProperty("documentId") final UUID documentId,
                                             @JsonProperty("reason") final String reason) {
        this.caseId = caseId;
        this.documentId = documentId;
        this.reason = reason;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getReason() {
        return reason;
    }
}
