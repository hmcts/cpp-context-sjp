package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.UpdateOffenceCodeRequestReceived;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class UpdateOffenceCodeRequestReceivedListener {

    @Inject
    private CaseRepository caseRepository;

    @Handles("sjp.events.update-offence-code-request-received")
    public void updateOffenceCodeRequestReceived(final Envelope<UpdateOffenceCodeRequestReceived> eventEnvelope) {
        final UpdateOffenceCodeRequestReceived payload = eventEnvelope.payload();

        final CaseDetail caseDetail = caseRepository.findBy(payload.getCaseId());

        payload.getUpdatedOffenceCodes().forEach(offenceCode -> {
            caseDetail.getDefendant()
                    .getOffences()
                    .stream()
                    .filter(offenceDetail -> offenceCode.getId().equals(offenceDetail.getId()))
                    .forEach(e -> e.setCode(offenceCode.getCode()));
        });


        caseRepository.save(caseDetail);
    }
}
