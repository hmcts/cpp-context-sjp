package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.listener.converter.DecisionSavedToCaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDecisionRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.Optional;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class CaseDecisionListener {

    @Inject
    private CaseDecisionRepository caseDecisionRepository;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private DecisionSavedToCaseDecision eventConverter;

    @Handles(DecisionSaved.EVENT_NAME)
    public void handleCaseDecisionSaved(final Envelope<DecisionSaved> envelope) {
        final CaseDecision caseDecision = eventConverter.convert(envelope.payload());
        final CaseDecision enrichedCaseDecision = enrichOffenceDecisionWithPleas(caseDecision);

        caseDecisionRepository.save(enrichedCaseDecision);
    }

    private CaseDecision enrichOffenceDecisionWithPleas(final CaseDecision caseDecision) {
        final CaseDetail caseDetails = caseRepository.findBy(caseDecision.getCaseId());

        caseDecision.getOffenceDecisions()
                .forEach(offenceDecision -> getOffence(caseDetails, offenceDecision).ifPresent(offence -> enrichOffenceDecision(offenceDecision, offence)));

        return caseDecision;
    }

    private static Optional<OffenceDetail> getOffence(final CaseDetail caseDetails, final OffenceDecision offenceDecision) {
        return caseDetails.getDefendant().getOffences().stream()
                .filter(offence -> offence.getId().equals(offenceDecision.getOffenceId()))
                .findFirst();
    }

    private static void enrichOffenceDecision(final OffenceDecision offenceDecision, final OffenceDetail offence) {
        offenceDecision.setPleaAtDecisionTime(offence.getPlea());
        offenceDecision.setPleaDate(offence.getPleaDate());
    }

}
