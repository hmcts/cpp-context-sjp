package uk.gov.moj.cpp.sjp.event.listener;


import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantPendingChangesAccepted;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetailUpdateRequest;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantDetailUpdateRequestRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class DefendantPendingChangesAcceptedListener {
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private DefendantDetailUpdateRequestRepository defendantDetailUpdateRequestRepository;
    @Handles("sjp.events.defendant-pending-changes-accepted")
    @Transactional
    public void defendantPendingChangesAccepted(final JsonEnvelope envelope) {

        final DefendantPendingChangesAccepted defendantPendingChangesAccepted = jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), DefendantPendingChangesAccepted.class);

        final DefendantDetailUpdateRequest defendantDetailUpdateRequest = defendantDetailUpdateRequestRepository.findBy(defendantPendingChangesAccepted.getCaseId());

        if (defendantDetailUpdateRequest == null) {
            throw new IllegalArgumentException("Unable to update defendant's details change request status of a case that does not exist: " + defendantPendingChangesAccepted.getCaseId());
        }

        defendantDetailUpdateRequest.setStatus(DefendantDetailUpdateRequest.Status.UPDATED);
        defendantDetailUpdateRequest.setUpdatedAt(defendantPendingChangesAccepted.getAcceptedAt());
        defendantDetailUpdateRequestRepository.save(defendantDetailUpdateRequest);
    }
}
