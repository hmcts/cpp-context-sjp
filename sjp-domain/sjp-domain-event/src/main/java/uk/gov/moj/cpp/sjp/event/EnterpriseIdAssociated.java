package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.util.UUID;

/**
 * Event: Signifies that an Enterprise ID has been associated with a Case.
 * @deprecated Enterprise ID now included in CaseReceived event
 */
@Deprecated
@Event("sjp.events.enterprise-id-associated")
public class EnterpriseIdAssociated {

    private UUID caseId;
    private String enterpriseId;

    public EnterpriseIdAssociated(final UUID caseId, final String enterpriseId) {
        this.caseId = caseId;
        this.enterpriseId = enterpriseId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getEnterpriseId() {
        return enterpriseId;
    }
}
