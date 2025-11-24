package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseEligibleForAOCP;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseService;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_LISTENER)
public class ResolveCaseEligibleForAOCPListener {

    @Inject
    private CaseService caseService;

    @Inject
    private JsonObjectToObjectConverter jsonObjectConverter;

    @Inject
    private OffenceRepository offenceRepository;

    @Handles(CaseEligibleForAOCP.EVENT_NAME)
    public void handleCaseEligibleForAOCP(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final CaseEligibleForAOCP caseEligibleForAOCP = jsonObjectConverter.convert(payload, CaseEligibleForAOCP.class);
        updateCaseDetails(caseEligibleForAOCP);
    }

    private void updateCaseDetails(final CaseEligibleForAOCP caseEligibleForAOCP) {
        final CaseDetail caseDetail = caseService.findById(caseEligibleForAOCP.getCaseId());
        caseDetail.setAocpEligible(true);
        caseDetail.setAocpVictimSurcharge(caseEligibleForAOCP.getVictimSurcharge());
        caseDetail.setAocpTotalCost(caseEligibleForAOCP.getAocpTotalCost());

        caseEligibleForAOCP.getAocpCostDefendant().getOffences().forEach(aocpOffence -> {
            final Optional<OffenceDetail> offenceDetail = caseDetail.getDefendant().getOffences().stream()
                    .filter(offence -> offence.getId().equals(aocpOffence.getId())).findFirst();
            if (offenceDetail.isPresent()) {
                offenceDetail.get().setAocpStandardPenalty(aocpOffence.getAocpStandardPenaltyAmount());
            }
        });
        caseService.saveCaseDetail(caseDetail);
    }
}
