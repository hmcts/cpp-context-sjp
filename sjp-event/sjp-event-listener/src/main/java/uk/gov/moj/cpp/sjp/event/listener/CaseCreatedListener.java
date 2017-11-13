package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.event.listener.converter.SjpCaseCreatedToCase;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class CaseCreatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private SjpCaseCreatedToCase sjpConverter;

    @Inject
    private CaseRepository caseRepository;

    @Handles("sjp.events.sjp-case-created")
    @Transactional
    public void sjpCaseCreated(final JsonEnvelope envelope) {
        CaseDetail caseDetail = sjpConverter.convert(jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), SjpCaseCreated.class));
        caseRepository.save(caseDetail);
    }
}
