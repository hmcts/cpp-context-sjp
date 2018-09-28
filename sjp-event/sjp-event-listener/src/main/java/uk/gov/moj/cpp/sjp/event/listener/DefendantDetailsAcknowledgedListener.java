package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdatesAcknowledged;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class DefendantDetailsAcknowledgedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    private CaseRepository caseRepository;

    @Handles(DefendantDetailsUpdatesAcknowledged.EVENT_NAME)
    public void defendantDetailsUpdatesAcknowledged(final JsonEnvelope envelope) {
        final DefendantDetailsUpdatesAcknowledged acknowledgementEvent =
                jsonObjectToObjectConverter.convert(
                        envelope.payloadAsJsonObject(),
                        DefendantDetailsUpdatesAcknowledged.class);

        final CaseDetail caseDetail = caseRepository.findBy(acknowledgementEvent.getCaseId());

        caseDetail.acknowledgeDefendantDetailsUpdates(acknowledgementEvent.getAcknowledgedAt());
    }
}
