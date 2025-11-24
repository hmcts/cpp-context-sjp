package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.TrialRequested;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class TrialRequestedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private OnlinePleaRepository.TrialOnlinePleaRepository onlinePleaRepository;

    @Inject
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @Handles("sjp.events.trial-requested")
    public void updateTrial(final JsonEnvelope event) {
        final TrialRequested trialRequested = jsonObjectConverter.convert(event.payloadAsJsonObject(), TrialRequested.class);
        final OnlinePlea onlinePlea = new OnlinePlea(trialRequested);
        onlinePleaRepository.saveOnlinePlea(onlinePlea);
        pendingDatesToAvoidRepository.save(new PendingDatesToAvoid(trialRequested.getCaseId(), trialRequested.getUpdatedDate()));
    }
}
