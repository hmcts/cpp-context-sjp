package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.SET_ASIDE;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.justice.json.schemas.domains.sjp.events.ApplicationDecisionSaved;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.decision.DecisionResubmitted;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.listener.converter.ApplicationDecisionSavedToApplicationDecision;
import uk.gov.moj.cpp.sjp.event.listener.converter.DecisionSavedToCaseDecision;
import uk.gov.moj.cpp.sjp.event.listener.service.CaseApplicationService;
import uk.gov.moj.cpp.sjp.persistence.entity.AccountNote;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseAccountNoteRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDecisionRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaDetailRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(EVENT_LISTENER)
public class CaseDecisionListener {

    @Inject
    private CaseDecisionRepository caseDecisionRepository;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private DecisionSavedToCaseDecision eventConverter;

    @Inject
    private ApplicationDecisionSavedToApplicationDecision applicationDecisionConverter;

    @Inject
    private CaseApplicationService caseApplicationService;

    @Inject
    private CaseAccountNoteRepository accountNoteRepository;

    @Inject
    OnlinePleaDetailRepository onlinePleaDetailRepository;

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    private static final List<VerdictType> CONVICTION_VERDICTS = asList(FOUND_GUILTY, PROVED_SJP);

    private static final String APPLICATION_DECISION_SAVED = "sjp.events.application-decision-saved";

    @Handles(DecisionSaved.EVENT_NAME)
    public void handleCaseDecisionSaved(final Envelope<DecisionSaved> envelope) {
        final DecisionSaved decisionSaved = envelope.payload();
        final CaseDecision caseDecision = eventConverter.convert(decisionSaved);
        final CaseDecision enrichedCaseDecision = enrichOffenceDecision(caseDecision);
        caseDecisionRepository.save(enrichedCaseDecision);
        final CaseDetail caseDetails = caseRepository.findBy(caseDecision.getCaseId());
        updateOffenceConvictionInformation(caseDetails, caseDecision);
        updateOffencePressRestriction(caseDetails, caseDecision);
        updateOffenceCompleted(caseDetails, caseDecision);

        if (nonNull(decisionSaved.getResultedThroughAOCP()) && Boolean.TRUE.equals(decisionSaved.getResultedThroughAOCP())) {
            caseDetails.setResultedThroughAOCP(true);
            saveAccountNotes(decisionSaved.getCaseId(), caseDetails.getUrn(), "Case resulted with AOCP");
            updateOnlinePleaDetails(caseDecision.getCaseId(), caseDetails.getDefendant().getId());
        }
        caseRepository.save(caseDetails);
    }

    @Handles(DecisionResubmitted.EVENT_NAME)
    public void handleCaseDecisionSavedWithPaymentTermsChanged(final Envelope<DecisionResubmitted> envelope) {
        // convert to the decision object
        final DecisionResubmitted decisionSavedWithPaymentTermsChanged = envelope.payload();
        final DecisionSaved decisionSaved = new DecisionSaved(
                decisionSavedWithPaymentTermsChanged.getDecisionSaved().getDecisionId(),
                decisionSavedWithPaymentTermsChanged.getDecisionSaved().getSessionId(),
                decisionSavedWithPaymentTermsChanged.getDecisionSaved().getCaseId(),
                decisionSavedWithPaymentTermsChanged.getDecisionSaved().getUrn(),
                decisionSavedWithPaymentTermsChanged.getDecisionSaved().getSavedAt(),
                decisionSavedWithPaymentTermsChanged.getDecisionSaved().getOffenceDecisions(),
                decisionSavedWithPaymentTermsChanged.getDecisionSaved().getFinancialImposition(),
                null,
                null,
                null,
                null);

        final CaseDecision caseDecision = eventConverter.convert(decisionSaved);
        final CaseDecision enrichedCaseDecision = enrichOffenceDecision(caseDecision);
        caseDecisionRepository.save(enrichedCaseDecision);
        final CaseDetail caseDetails = caseRepository.findBy(caseDecision.getCaseId());
        updateOffenceConvictionInformation(caseDetails, caseDecision);
        updateOffencePressRestriction(caseDetails, caseDecision);
        updateOffenceCompleted(caseDetails, caseDecision);
        caseRepository.save(caseDetails);
        saveAccountNotes(caseDetails.getId(), caseDetails.getUrn(), decisionSavedWithPaymentTermsChanged.getAccountNote());
    }

    private void saveAccountNotes(final UUID caseId,
                                  final String caseUrn,
                                  final String accountNoteText) {
        final AccountNote accountNote = new AccountNote();
        accountNote.setId(UUID.randomUUID());
        accountNote.setCaseId(caseId);
        accountNote.setCaseUrn(caseUrn);
        accountNote.setNoteText(accountNoteText);
        accountNoteRepository.save(accountNote);
    }

    @Handles(APPLICATION_DECISION_SAVED)
    public void handleApplicationDecisionSaved(final Envelope<ApplicationDecisionSaved> envelope) {
        final CaseApplicationDecision applicationDecision = applicationDecisionConverter.convert(envelope.payload());
        caseApplicationService.saveCaseApplicationDecision(applicationDecision);
    }

    private void updateOffenceCompleted(final CaseDetail caseDetails, final CaseDecision caseDecision) {
        caseDecision.getOffenceDecisions().stream()
                .forEach(offenceDecision -> getOffence(caseDetails, offenceDecision)
                        .ifPresent(offenceDetail -> offenceDetail.setCompleted(offenceDecision.getDecisionType().isFinal())));
    }

    private void updateOffencePressRestriction(final CaseDetail caseDetails, final CaseDecision caseDecision) {
        caseDecision.getOffenceDecisions().stream()
                .filter(offenceDecision -> nonNull(offenceDecision.getPressRestriction()))
                .forEach(offenceDecision -> getOffence(caseDetails, offenceDecision)
                        .ifPresent(offenceDetail -> offenceDetail.setPressRestriction(offenceDecision.getPressRestriction())));
    }

    private void updateOffenceConvictionInformation(final CaseDetail caseDetails, CaseDecision caseDecision) {
        caseDecision.getOffenceDecisions().stream()
                .filter(offenceDecision -> nonNull(offenceDecision.getVerdictType()))
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

    private void updateOnlinePleaDetails(final UUID caseId, final UUID defendantId) {
        final List<OnlinePleaDetail> onlinePleaDetails = onlinePleaDetailRepository.findByCaseIdAndDefendantId(caseId, defendantId);
        onlinePleaDetails.forEach(onlinePleaDetail -> {
            onlinePleaDetail.setPlea(PleaType.GUILTY);
            onlinePleaDetailRepository.save(onlinePleaDetail);
        });
    }

}
