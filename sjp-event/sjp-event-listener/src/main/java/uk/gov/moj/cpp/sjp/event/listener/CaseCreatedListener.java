package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.SjpCaseCreated;
import uk.gov.moj.cpp.sjp.event.listener.converter.CaseReceivedToCase;
import uk.gov.moj.cpp.sjp.event.listener.converter.SjpCaseCreatedToCase;
import uk.gov.moj.cpp.sjp.event.listener.handler.CaseSearchResultService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class CaseCreatedListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Inject
    @SuppressWarnings("deprecation")
    private SjpCaseCreatedToCase sjpConverter;

    @Inject
    private CaseReceivedToCase caseReceivedToCaseConverter;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private CaseSearchResultService caseSearchResultService;

    /**
     * @deprecated required just for legacy events, replaced by
     * {@link #caseReceived(JsonEnvelope)} which contains {@link DefendantDetail} as the part of the event
     */
    @Deprecated
    @Transactional
    @Handles("sjp.events.sjp-case-created")
    public void sjpCaseCreated(final JsonEnvelope envelope) {
        CaseDetail caseDetail = sjpConverter.convert(
                jsonObjectToObjectConverter.convert(
                        envelope.payloadAsJsonObject(), SjpCaseCreated.class
                )
        );
        caseRepository.save(caseDetail);
    }

    @Handles(CaseReceived.EVENT_NAME)
    @Transactional
    public void caseReceived(final JsonEnvelope envelope) {
        CaseDetail caseDetail = caseReceivedToCaseConverter.convert(
                jsonObjectToObjectConverter.convert(
                        envelope.payloadAsJsonObject(), CaseReceived.class
                )
        );

        caseRepository.save(caseDetail);

        caseSearchResultService.onDefendantDetailsUpdated(
                caseDetail.getId(),
                caseDetail.getDefendant().getPersonalDetails().getFirstName(),
                caseDetail.getDefendant().getPersonalDetails().getLastName(),
                caseDetail.getDefendant().getPersonalDetails().getDateOfBirth()
        );
    }
}
