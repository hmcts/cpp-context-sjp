package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.event.PleasSet;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.Optional;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class PleasSetListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private CaseRepository caseRepository;

    @Handles(PleasSet.EVENT_NAME)
    @Transactional
    public void updateDisabilityNeeds(final JsonEnvelope event) {
        final PleasSet pleasSet = jsonObjectConverter.convert(event.payloadAsJsonObject(), PleasSet.class);
        final CaseDetail caseDetail = caseRepository.findBy(pleasSet.getCaseId());
        final String disabilityNeeds = Optional.ofNullable(pleasSet.getDefendantCourtOptions())
                .map(DefendantCourtOptions::getDisabilityNeeds)
                .map(DisabilityNeeds::getDisabilityNeeds)
                .orElse(null);
        caseDetail.getDefendant().setDisabilityNeeds(disabilityNeeds);
        caseRepository.save(caseDetail);
    }

}
