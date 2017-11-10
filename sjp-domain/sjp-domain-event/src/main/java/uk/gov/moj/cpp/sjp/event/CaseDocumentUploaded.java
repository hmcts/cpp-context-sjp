package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("structure.events.case-document-uploaded")
public class CaseDocumentUploaded {

    private final UUID caseId;

    private final UUID documentReference;

    private final String documentType;

    public CaseDocumentUploaded(UUID caseId, UUID documentReference, String documentType) {
        this.caseId = caseId;
        this.documentReference = documentReference;
        this.documentType = documentType;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getDocumentReference() {
        return documentReference;
    }

    public String getDocumentType() {
        return documentType;
    }
}
