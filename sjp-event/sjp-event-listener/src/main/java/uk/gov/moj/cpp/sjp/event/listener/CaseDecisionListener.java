package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Arrays.asList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.SET_ASIDE;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.listener.converter.DecisionSavedToCaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDecisionRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.List;
import java.util.Objects;
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

    private static final List<VerdictType> CONVICTION_VERDICTS = asList(FOUND_GUILTY, PROVED_SJP);

    @Handles(DecisionSaved.EVENT_NAME)
    public void handleCaseDecisionSaved(final Envelope<DecisionSaved> envelope) {
        final CaseDecision caseDecision = eventConverter.convert(envelope.payload());
        final CaseDecision enrichedCaseDecision = enrichOffenceDecision(caseDecision);
        caseDecisionRepository.save(enrichedCaseDecision);
        updateOffenceConvictionInformation(caseDecision);
    }

    private void updateOffenceConvictionInformation(final CaseDecision caseDecision) {
        final CaseDetail caseDetails = caseRepository.findBy(caseDecision.getCaseId());
        caseDecision.getOffenceDecisions().stream()
                .filter(offenceDecision -> Objects.nonNull(offenceDecision.getVerdictType()))
                .filter(offenceDecision -> CONVICTION_VERDICTS.contains(offenceDecision.getVerdictType()))
                .forEach(offenceDecision -> getOffence(caseDetails, offenceDecision).ifPresent(offenceDetail -> {
                    offenceDetail.setConviction(offenceDecision.getVerdictType());
                    offenceDetail.setConvictionDate(caseDecision.getSavedAt().toLocalDate());
                }));

        caseDecision
                .getOffenceDecisions()
                .stream()
                .filter(offenceDecision -> SET_ASIDE.equals(offenceDecision.getDecisionType()))
                .forEach(offenceDecision -> getOffence(caseDetails, offenceDecision)
                        .ifPresent(offenceDetail -> {
                            offenceDetail.setConviction(null);
                            offenceDetail.setConvictionDate(null);
                        })
                );

        caseRepository.save(caseDetails);
    }

    private CaseDecision enrichOffenceDecision(final CaseDecision caseDecision) {
        final CaseDetail caseDetails = caseRepository.findBy(caseDecision.getCaseId());

        // enrich with pleas information
        caseDecision.getOffenceDecisions()
                .forEach(offenceDecision -> getOffence(caseDetails, offenceDecision).ifPresent(offence -> enrichOffenceDecision(offenceDecision, offence)));

        // set the set-aside flag
        final boolean caseSetAside = caseDecision
                .getOffenceDecisions()
                .stream()
                .allMatch(e -> SET_ASIDE.equals(e.getDecisionType()));
        caseDetails.setSetAside(caseSetAside);

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
