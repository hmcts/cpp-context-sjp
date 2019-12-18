package uk.gov.moj.cpp.sjp.event.listener;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.UUID;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

@ServiceComponent(EVENT_LISTENER)
public class OnlinePleaReceivedListener {

    @Inject
    private CaseRepository caseRepository;

    private static final String CASE_ID_PROPERTY = "caseId";

    @Handles("sjp.events.online-plea-received")
    public void onlinePleaReceived(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(payload.getString(CASE_ID_PROPERTY));

        CaseDetail caseDetail = caseRepository.findBy(caseId);
        caseDetail.setOnlinePleaReceived(true);
        caseRepository.save(caseDetail);
    }
}
