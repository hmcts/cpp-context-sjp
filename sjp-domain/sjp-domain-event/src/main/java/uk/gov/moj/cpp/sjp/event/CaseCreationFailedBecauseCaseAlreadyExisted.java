package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

@Event("structure.events.case-creation-failed-because-case-already-existed")
public class CaseCreationFailedBecauseCaseAlreadyExisted {

    private UUID caseId;

    private String urn;

    public CaseCreationFailedBecauseCaseAlreadyExisted(UUID caseId, String urn) {
        this.caseId = caseId;
        this.urn = urn;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getUrn() {
        return urn;
    }
}
