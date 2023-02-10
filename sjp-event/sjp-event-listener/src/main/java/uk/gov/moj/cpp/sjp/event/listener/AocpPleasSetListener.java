package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.AocpPleasSet;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class AocpPleasSetListener {

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private CaseService caseService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Transactional
    @Handles(AocpPleasSet.EVENT_NAME)
    public void handleAocpPleasSet(final JsonEnvelope event) {

        final AocpPleasSet aocpPleasSet = jsonObjectConverter.convert(event.payloadAsJsonObject(), AocpPleasSet.class);
        final CaseDetail caseDetail = caseService.findById(aocpPleasSet.getCaseId());
        aocpPleasSet.getPleas().forEach(plea -> {
            final Optional<OffenceDetail> offenceDetail = caseDetail.getDefendant().getOffences().stream()
                    .filter(offence -> offence.getId().equals(plea.getOffenceId())).findFirst();
            if (offenceDetail.isPresent()) {
                offenceDetail.get().setPlea(plea.getPleaType());
                offenceDetail.get().setPleaDate(aocpPleasSet.getPleaDate());
                offenceDetail.get().setPleaMethod(aocpPleasSet.getPleaMethod());
            }
        });
        caseService.saveCaseDetail(caseDetail);
    }
}