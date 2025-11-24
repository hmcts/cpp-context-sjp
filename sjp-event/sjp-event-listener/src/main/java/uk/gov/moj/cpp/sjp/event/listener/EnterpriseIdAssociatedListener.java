package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_LISTENER)
public class EnterpriseIdAssociatedListener {

    private static final String CASE_ID_PROPERTY = "caseId";
    private static final String ENTERPRISE_ID_PROPERTY = "enterpriseId";

    @Inject
    private CaseRepository caseRepository;

    @Handles("sjp.events.enterprise-id-associated")
    public void associateEnterpriseIdToCase(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString(CASE_ID_PROPERTY));
        final String enterpriseId = payload.getString(ENTERPRISE_ID_PROPERTY);

        final CaseDetail caseDetail = caseRepository.findBy(caseId);
        caseDetail.setEnterpriseId(enterpriseId);
    }
}
