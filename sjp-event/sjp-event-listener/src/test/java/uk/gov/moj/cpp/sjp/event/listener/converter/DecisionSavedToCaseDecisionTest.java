package uk.gov.moj.cpp.sjp.event.listener.converter;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty.*;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.WEEK;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.ReasonForDeductingFromBenefits.DEFENDANT_REQUESTED;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.LumpSum;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.persistence.entity.AdjournOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DismissOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferForCourtHearingDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionSavedToCaseDecisionTest {

    @InjectMocks
    private DecisionSavedToCaseDecision converter;

    @Mock
    private EntityManager em;

    @Mock
    private Session session;

    private UUID caseDecisionId = randomUUID();

    @Test
    public void shouldConvertDecisionSavedWithWithdrawOffenceToCaseDecision() {
        shouldConvertDecisionSaved(WITHDRAW, WITHDRAW);
    }

    @Test
    public void shouldConvertDecisionSavedWithReferForCourtHearingDecision() {
        shouldConvertDecisionSaved(REFER_FOR_COURT_HEARING);
    }

    @Test
    public void shouldConvertDecisionSavedWithAdjournOffenceToCaseDecision() {
        shouldConvertDecisionSaved(ADJOURN, ADJOURN);
    }

    @Test
    public void shouldConvertDecisionSavedWithAdjournAndWithdrawOffenceToCaseDecision() {
        shouldConvertDecisionSaved(ADJOURN, WITHDRAW, REFER_FOR_COURT_HEARING);
    }

    @Test
    public void shouldConvertDecisionSavedWithDischargeAndFinancialPenaltyOffenceToCaseDecision() {
        shouldConvertDecisionSaved(DISCHARGE, FINANCIAL_PENALTY);
    }

    @Test
    public void shouldConvertDecisionSavedWithAdjournAndDismissToCaseDecision() {
        shouldConvertDecisionSaved(ADJOURN, DISMISS);
    }

    private void shouldConvertDecisionSaved(final DecisionType... decisionTypes) {

        final DecisionSaved event = buildDecisionSavedEvent(buildMultipleOffenceDecisionsWith(asList(decisionTypes)));

        when(em.getReference(Session.class, event.getSessionId())).thenReturn(session);

        final CaseDecision actualCaseDecision = converter.convert(event);

        final CaseDecision expectedCaseDecision = buildExpectedCaseDecision(event);

        thenCaseDecisionShouldMatchCorrectly(actualCaseDecision, expectedCaseDecision);
    }

    private void thenCaseDecisionShouldMatchCorrectly(final CaseDecision actualCaseDecision, final CaseDecision expectedCaseDecision) {
        thenOffenceDecisionsShouldMatch(expectedCaseDecision.getOffenceDecisions(), actualCaseDecision.getOffenceDecisions());

        assertEquals(expectedCaseDecision.getId(), actualCaseDecision.getId());
        assertEquals(expectedCaseDecision.getCaseId(), actualCaseDecision.getCaseId());
        assertEquals(expectedCaseDecision.getSession(), actualCaseDecision.getSession());
        assertEquals(expectedCaseDecision.getSavedAt(), actualCaseDecision.getSavedAt());
    }

    private void thenOffenceDecisionsShouldMatch(List<uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision> expectedOffenceDecisions, List<uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision> actualOffenceDecisions) {

        for (int i = 0; i < expectedOffenceDecisions.size(); i++) {
            assertTrue(reflectionEquals(actualOffenceDecisions.get(i), expectedOffenceDecisions.get(i), "dischargePeriod"));
        }
    }

    private DecisionSaved buildDecisionSavedEvent(final List<OffenceDecision> offenceDecisions) {
        return new DecisionSaved(caseDecisionId, randomUUID(), randomUUID(), now(), offenceDecisions);
    }

    private List<OffenceDecision> buildMultipleOffenceDecisionsWith(final List<DecisionType> decisionTypes) {
        List<OffenceDecision> offenceDecisions = newArrayList();
        decisionTypes.forEach(decisionType -> {
            switch (decisionType) {
                case ADJOURN:
                    offenceDecisions.add(new Adjourn(randomUUID(), asList(createOffenceDecisionInformation(randomUUID(), NO_VERDICT)), "Not enough documents for decision", LocalDate.now()));
                    break;
                case WITHDRAW:
                    offenceDecisions.add(new Withdraw(randomUUID(), createOffenceDecisionInformation(randomUUID(), NO_VERDICT), randomUUID()));
                    break;
                case REFER_FOR_COURT_HEARING:
                    offenceDecisions.add(new ReferForCourtHearing(randomUUID(), asList(createOffenceDecisionInformation(randomUUID(), PROVED_SJP)), randomUUID(), "Listing notes", 30, null));
                    break;
                case DISMISS:
                    offenceDecisions.add(new Dismiss(randomUUID(), createOffenceDecisionInformation(randomUUID(), FOUND_NOT_GUILTY)));
                    break;
                case DISCHARGE:
                    offenceDecisions.add(Discharge.createDischarge(randomUUID(),
                            createOffenceDecisionInformation(randomUUID(), FOUND_GUILTY),
                            DischargeType.CONDITIONAL,
                            new DischargePeriod(10, WEEK),
                            BigDecimal.valueOf(20.3), null, true,
                            null
                    ));
                    break;
                case FINANCIAL_PENALTY:
                    offenceDecisions.add(createFinancialPenalty(randomUUID(),
                            createOffenceDecisionInformation(randomUUID(), FOUND_GUILTY),
                            BigDecimal.valueOf(20.3),
                            BigDecimal.valueOf(20.4),
                            null,
                            true,
                            null, null));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        });
        return offenceDecisions;
    }

    private CaseDecision buildExpectedCaseDecision(final DecisionSaved event) {
        final CaseDecision caseDecision = new CaseDecision();
        caseDecision.setId(caseDecisionId);
        caseDecision.setCaseId(event.getCaseId());
        caseDecision.setSession(session);
        caseDecision.setSavedAt(event.getSavedAt());
        caseDecision.setOffenceDecisions(buildExpectedOffenceDecisions(event.getOffenceDecisions()));
        return caseDecision;
    }

    private List<uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision> buildExpectedOffenceDecisions(List<OffenceDecision> offenceDecisions) {
        return offenceDecisions.stream().flatMap(this::getExpectedOffenceDecision).collect(toList());
    }

    private Stream<uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision> getExpectedOffenceDecision(final OffenceDecision offenceDecision) {
        switch (offenceDecision.getType()) {
            case ADJOURN:
                final Adjourn adjourn = (Adjourn) offenceDecision;
                return adjourn.getOffenceDecisionInformation().stream()
                        .map(offenceDecisionInformation -> new AdjournOffenceDecision(
                                offenceDecisionInformation.getOffenceId(),
                                caseDecisionId,
                                adjourn.getReason(),
                                adjourn.getAdjournTo(), NO_VERDICT, null));
            case WITHDRAW:
                final Withdraw withdraw = (Withdraw) offenceDecision;
                final WithdrawOffenceDecision withdrawEntity = new WithdrawOffenceDecision(withdraw.getOffenceDecisionInformation().getOffenceId(), caseDecisionId, withdraw.getWithdrawalReasonId(), NO_VERDICT);
                return Stream.of(withdrawEntity);
            case REFER_FOR_COURT_HEARING:
                final ReferForCourtHearing referForCourtHearing = (ReferForCourtHearing) offenceDecision;
                return referForCourtHearing.getOffenceDecisionInformation().stream()
                        .map(offenceDecisionInformation -> new ReferForCourtHearingDecision(
                                offenceDecisionInformation.getOffenceId(),
                                caseDecisionId,
                                referForCourtHearing.getReferralReasonId(),
                                referForCourtHearing.getEstimatedHearingDuration(),
                                referForCourtHearing.getListingNotes(),
                                PROVED_SJP, null));
            case DISMISS:
                final Dismiss dismiss = (Dismiss) offenceDecision;
                return Stream.of(new DismissOffenceDecision(dismiss.getOffenceDecisionInformation().getOffenceId(), caseDecisionId, FOUND_NOT_GUILTY));
            case DISCHARGE:
                final Discharge discharge = (Discharge) offenceDecision;
                return Stream.of(new DischargeOffenceDecision(discharge.getOffenceDecisionInformation().getOffenceId(),
                        caseDecisionId,
                        FOUND_GUILTY,
                        new uk.gov.moj.cpp.sjp.persistence.entity.DischargePeriod(WEEK, 10),
                        true, BigDecimal.valueOf(20.3),
                        null,
                        DischargeType.CONDITIONAL,
                        null, null
                ));
            case FINANCIAL_PENALTY:
                final FinancialPenalty financialPenalty = (FinancialPenalty) offenceDecision;
                return Stream.of(new FinancialPenaltyOffenceDecision(financialPenalty.getOffenceDecisionInformation().getOffenceId(),
                        caseDecisionId,
                        FOUND_GUILTY,
                        true,
                        BigDecimal.valueOf(20.4),
                        null,
                        BigDecimal.valueOf(20.3),
                        null, null, null
                ));
            default:
                throw new UnsupportedOperationException();
        }
    }

    private FinancialImposition buildFinancialImposition() {
        FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(BigDecimal.valueOf(200.23), null, BigDecimal.valueOf(300),
                        null, null, false),
                new Payment(BigDecimal.valueOf(300), PAY_TO_COURT,
                        "some reason", DEFENDANT_REQUESTED,
                        new PaymentTerms(false,
                                new LumpSum(BigDecimal.valueOf(30), 20, LocalDate.now()),
                                new Installments(BigDecimal.valueOf(40), InstallmentPeriod.WEEKLY, LocalDate.now())),
                        null
                ));
        return financialImposition;
    }
}
