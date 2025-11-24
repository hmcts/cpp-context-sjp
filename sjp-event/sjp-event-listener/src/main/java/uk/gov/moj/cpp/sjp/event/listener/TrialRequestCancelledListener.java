package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class TrialRequestCancelledListener {

    @Inject
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @Handles("sjp.events.trial-request-cancelled")
    public void cancelTrial(final JsonEnvelope event) {
        final UUID caseId = UUID.fromString(event.payloadAsJsonObject().getString("caseId"));

        pendingDatesToAvoidRepository.removeByCaseId(caseId);
    }

}