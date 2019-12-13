package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.OutstandingFinesUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class OutstandingFinesUpdatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private OnlinePleaRepository.OutstandingFinesOnlinePleaRepository outstandingFinesOnlinePleaRepository;

    @Handles(OutstandingFinesUpdated.EVENT_NAME)
    @Transactional
    public void updateOutstandingFines(final JsonEnvelope envelope) {
        final OutstandingFinesUpdated event = jsonObjectConverter.convert(envelope.payloadAsJsonObject(), OutstandingFinesUpdated.class);
        final OnlinePlea onlinePlea = new OnlinePlea(event);
        outstandingFinesOnlinePleaRepository.saveOnlinePlea(onlinePlea);
    }
}
