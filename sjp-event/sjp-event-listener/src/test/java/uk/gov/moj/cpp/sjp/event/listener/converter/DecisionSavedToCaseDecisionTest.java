package uk.gov.moj.cpp.sjp.event.listener.converter;

import static com.google.common.collect.Lists.newArrayList;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISCHARGE;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.WITHDRAW;
import static uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty.createFinancialPenalty;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.WEEK;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.NoSeparatePenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredForFutureSJPSession;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredToOpenCourt;
import uk.gov.moj.cpp.sjp.domain.decision.SetAside;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.testutils.builders.AdjournBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.builders.ReferForCourtHearingBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.builders.ReferToOpenCourtBuilder;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.persistence.entity.AdjournOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DismissOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferForCourtHearingDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;
import uk.gov.moj.cpp.sjp.util.fakes.TestOffenceRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionSavedToCaseDecisionTest {

    private static final String IGNORED_FIELDS = "dischargePeriod";
    private static final PressRestriction NULL_PRESS_RESTRICTION = null;
    private final UUID decisionId = randomUUID();
    private final UUID sessionId = randomUUID();
    private final UUID caseId = randomUUID();
    private final ZonedDateTime now = now();

    @InjectMocks
    private DecisionSavedToCaseDecision converter;
    @Spy
    private TestOffenceRepository offenceRepository;
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

    @Test
    public void shouldConvertPressRestrictionRequested() {
        final PressRestriction pressRestriction = PressRestriction.requested("My Name");
        final DecisionSaved event = decisionSavedEvent(
                financialPenalty(pressRestriction),
                dismiss(pressRestriction),
                withdraw(pressRestriction),
                discharge(pressRestriction),
                noSeparatePenalty(pressRestriction)
        );

        final CaseDecision actual = converter.convert(event);

        assertThat(actual.getOffenceDecisions(), everyItem(hasPressRestriction(pressRestriction)));
    }

    @Test
    public void shouldConvertPressRestrictionRevoked() {
        final PressRestriction pressRestriction = PressRestriction.revoked();
        final DecisionSaved event = decisionSavedEvent(
                financialPenalty(pressRestriction),
                dismiss(pressRestriction),
                withdraw(pressRestriction),
                discharge(pressRestriction),
                noSeparatePenalty(pressRestriction)
        );

        final CaseDecision actual = converter.convert(event);

        assertThat(actual.getOffenceDecisions(), everyItem(hasPressRestriction(pressRestriction)));
    }

    @Test
    public void shouldConvertPressRestrictionNotPresent() {
        final DecisionSaved event = decisionSavedEvent(
                financialPenalty(NULL_PRESS_RESTRICTION),
                adjourn(NULL_PRESS_RESTRICTION),
                referToCourtHearing(NULL_PRESS_RESTRICTION),
                dismiss(NULL_PRESS_RESTRICTION),
                withdraw(NULL_PRESS_RESTRICTION),
                discharge(NULL_PRESS_RESTRICTION),
                noSeparatePenalty(NULL_PRESS_RESTRICTION),
                setAside(NULL_PRESS_RESTRICTION),
                referredToOpenCourt(NULL_PRESS_RESTRICTION),
                referredForFutureSJPSession(UUID.randomUUID(), UUID.randomUUID(), NULL_PRESS_RESTRICTION)
        );

        final CaseDecision actual = converter.convert(event);

        assertThat(actual.getOffenceDecisions(), everyItem(hasProperty("pressRestriction", nullValue())));
    }

    @Test
    public void shouldConvertAdjournDecisionWithMixedRestrictableAndNonRestrictableOffences() {
        final OffenceDetail offence1 = offence(true);
        final OffenceDetail offence2 = offence(false);
        offenceRepository.addOffences(offence1, offence2);
        final Adjourn adjourn = AdjournBuilder.withDefaults()
                .addOffenceDecisionInformation(offence1.getId(), NO_VERDICT)
                .addOffenceDecisionInformation(offence2.getId(), NO_VERDICT)
                .pressRestriction("A Name")
                .build();
        final DecisionSaved event = decisionSavedEvent(adjourn);

        final CaseDecision actual = converter.convert(event);

        assertThat(actual.getOffenceDecisions().get(0), hasPressRestriction(PressRestriction.requested("A Name")));
        assertThat(actual.getOffenceDecisions().get(1).getPressRestriction(), nullValue());
    }

    @Test
    public void shouldConvertReferForCourtHearingDecisionWithMixedRestrictableAndNonRestrictableOffences() {
        final OffenceDetail offence1 = offence(true);
        final OffenceDetail offence2 = offence(false);
        offenceRepository.addOffences(offence1, offence2);
        final ReferForCourtHearing referForCourtHearing = ReferForCourtHearingBuilder.withDefaults()
                .addOffenceDecisionInformation(offence1.getId(), NO_VERDICT)
                .addOffenceDecisionInformation(offence2.getId(), NO_VERDICT)
                .pressRestriction("A Name")
                .build();
        final DecisionSaved event = decisionSavedEvent(referForCourtHearing);

        final CaseDecision actual = converter.convert(event);

        assertThat(actual.getOffenceDecisions().get(0), hasPressRestriction(PressRestriction.requested("A Name")));
        assertThat(actual.getOffenceDecisions().get(1).getPressRestriction(), nullValue());
    }

    @Test
    public void shouldConvertReferredToOpenCourtDecisionWithMixedRestrictableAndNonRestrictableOffences() {
        final OffenceDetail offence1 = offence(true);
        final OffenceDetail offence2 = offence(false);
        offenceRepository.addOffences(offence1, offence2);
        final ReferredToOpenCourt referredToOpenCourt = ReferToOpenCourtBuilder.withDefaults()
                .addOffenceDecisionInformation(offence1.getId(), PROVED_SJP)
                .addOffenceDecisionInformation(offence2.getId(), PROVED_SJP)
                .pressRestriction("A Name")
                .build();
        final DecisionSaved event = decisionSavedEvent(referredToOpenCourt);

        final CaseDecision actual = converter.convert(event);

        assertThat(actual.getOffenceDecisions().get(0), hasPressRestriction(PressRestriction.requested("A Name")));
        assertThat(actual.getOffenceDecisions().get(1).getPressRestriction(), nullValue());
    }

    @Test
    public void shouldConvertReferredForFutureSJPSessionDecisionWithMixedRestrictableAndNonRestrictableOffences() {
        final OffenceDetail offence1 = offence(true);
        final OffenceDetail offence2 = offence(false);
        offenceRepository.addOffences(offence1, offence2);
        final ReferredForFutureSJPSession referredForFutureSJPSession = referredForFutureSJPSession(
                offence1.getId(),
                offence2.getId(),
                PressRestriction.requested("A Name")
        );
        final DecisionSaved event = decisionSavedEvent(referredForFutureSJPSession);

        final CaseDecision actual = converter.convert(event);

        assertThat(actual.getOffenceDecisions().get(0), hasPressRestriction(PressRestriction.requested("A Name")));
        assertThat(actual.getOffenceDecisions().get(1).getPressRestriction(), nullValue());
    }

    @Test
    public void shouldConvertSetAsideDecisionWithMixedRestrictableAndNonRestrictableOffences() {
        final OffenceDetail offence1 = offence(true);
        final OffenceDetail offence2 = offence(false);
        offenceRepository.addOffences(offence1, offence2);
        final SetAside setAside = setAside(
                offence1.getId(),
                offence2.getId(),
                PressRestriction.requested("A Name")
        );
        final DecisionSaved event = decisionSavedEvent(setAside);

        final CaseDecision actual = converter.convert(event);

        assertThat(actual.getOffenceDecisions().get(0), hasPressRestriction(PressRestriction.requested("A Name")));
        assertThat(actual.getOffenceDecisions().get(1).getPressRestriction(), nullValue());
    }

    private OffenceDetail offence(final boolean pressRestrictable) {
        return offence(randomUUID(), pressRestrictable);
    }

    private OffenceDetail offence(final UUID offenceId, final boolean pressRestrictable) {
        return new OffenceDetail.OffenceDetailBuilder()
                .setId(offenceId)
                .withPressRestrictable(pressRestrictable).build();
    }

    private void shouldConvertDecisionSaved(final DecisionType... decisionTypes) {
        final DecisionSaved event = buildDecisionSavedEvent(buildMultipleOffenceDecisionsWith(asList(decisionTypes)));
        mockOffenceRepository(event);
        when(em.getReference(Session.class, event.getSessionId())).thenReturn(session);

        final CaseDecision actualCaseDecision = converter.convert(event);

        final CaseDecision expectedCaseDecision = buildExpectedCaseDecision(event);

        thenCaseDecisionShouldMatchCorrectly(actualCaseDecision, expectedCaseDecision);
    }

    private void mockOffenceRepository(final DecisionSaved event) {
        event.getOffenceDecisions().stream()
                .flatMap(offenceDecision -> offenceDecision.getOffenceIds().stream())
                .map(offenceId -> offence(offenceId, false))
                .forEach(offenceRepository::addOffences);
    }

    private Matcher hasPressRestriction(final PressRestriction pressRestriction) {
        return allOf(
                hasProperty("pressRestriction", allOf(
                        hasProperty("name", equalTo(pressRestriction.getName())),
                        hasProperty("requested", equalTo(pressRestriction.getRequested()))
                ))
        );
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
            assertTrue(reflectionEquals(actualOffenceDecisions.get(i), expectedOffenceDecisions.get(i), IGNORED_FIELDS));
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
                    offenceDecisions.add(adjourn(NULL_PRESS_RESTRICTION));
                    break;
                case WITHDRAW:
                    offenceDecisions.add(withdraw(NULL_PRESS_RESTRICTION));
                    break;
                case REFER_FOR_COURT_HEARING:
                    offenceDecisions.add(referToCourtHearing(NULL_PRESS_RESTRICTION));
                    break;
                case DISMISS:
                    offenceDecisions.add(dismiss(NULL_PRESS_RESTRICTION));
                    break;
                case DISCHARGE:
                    offenceDecisions.add(discharge(NULL_PRESS_RESTRICTION));
                    break;
                case FINANCIAL_PENALTY:
                    offenceDecisions.add(financialPenalty(NULL_PRESS_RESTRICTION));
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
        final uk.gov.moj.cpp.sjp.persistence.entity.PressRestriction pressRestriction =
                toPressRestrictionEntity(offenceDecision.getPressRestriction()).orElse(null);

        switch (offenceDecision.getType()) {
            case ADJOURN:
                final Adjourn adjourn = (Adjourn) offenceDecision;
                return adjourn.getOffenceDecisionInformation().stream()
                        .map(offenceDecisionInformation -> new AdjournOffenceDecision(
                                offenceDecisionInformation.getOffenceId(),
                                caseDecisionId,
                                adjourn.getReason(),
                                adjourn.getAdjournTo(),
                                NO_VERDICT,
                                null,
                                pressRestriction));
            case WITHDRAW:
                final Withdraw withdraw = (Withdraw) offenceDecision;
                final WithdrawOffenceDecision withdrawEntity = new WithdrawOffenceDecision(withdraw.getOffenceDecisionInformation().getOffenceId(), caseDecisionId, withdraw.getWithdrawalReasonId(), NO_VERDICT, null);
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
                                PROVED_SJP,
                                null, null));
            case DISMISS:
                final Dismiss dismiss = (Dismiss) offenceDecision;
                return Stream.of(new DismissOffenceDecision(dismiss.getOffenceDecisionInformation().getOffenceId(), caseDecisionId, FOUND_NOT_GUILTY, null));
            case DISCHARGE:
                final Discharge discharge = (Discharge) offenceDecision;
                return Stream.of(new DischargeOffenceDecision(discharge.getOffenceDecisionInformation().getOffenceId(),
                        caseDecisionId,
                        FOUND_GUILTY,
                        new uk.gov.moj.cpp.sjp.persistence.entity.DischargePeriod(WEEK, 10),
                        true, BigDecimal.valueOf(20.3),
                        null,
                        DischargeType.CONDITIONAL,
                        null,
                        null
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
                        null,
                        null,
                        null,
                        pressRestriction));
            default:
                throw new UnsupportedOperationException();
        }
    }

    private DecisionSaved decisionSavedEvent(final OffenceDecision... offenceDecisions) {
        when(em.getReference(Session.class, sessionId)).thenReturn(session);
        return new DecisionSaved(decisionId, sessionId, caseId, now, Arrays.asList(offenceDecisions));
    }

    private FinancialPenalty financialPenalty(final PressRestriction pressRestriction) {
        return createFinancialPenalty(randomUUID(),
                createOffenceDecisionInformation(randomUUID(), FOUND_GUILTY),
                BigDecimal.valueOf(20.3),
                BigDecimal.valueOf(20.4),
                null,
                true,
                null,
                null,
                pressRestriction);
    }

    private Discharge discharge(final PressRestriction pressRestriction) {
        return Discharge.createDischarge(randomUUID(),
                createOffenceDecisionInformation(randomUUID(), FOUND_GUILTY),
                DischargeType.CONDITIONAL,
                new DischargePeriod(10, WEEK),
                BigDecimal.valueOf(20.3), null, true,
                null,
                pressRestriction
        );
    }

    private Dismiss dismiss(final PressRestriction pressRestriction) {
        return new Dismiss(randomUUID(), createOffenceDecisionInformation(randomUUID(), FOUND_NOT_GUILTY), pressRestriction);
    }

    private ReferForCourtHearing referToCourtHearing(final PressRestriction pressRestriction) {
        return new ReferForCourtHearing(randomUUID(), asList(createOffenceDecisionInformation(randomUUID(), PROVED_SJP)), randomUUID(),null, "Listing notes", 30, null, pressRestriction, null);
    }

    private Withdraw withdraw(final PressRestriction pressRestriction) {
        return new Withdraw(randomUUID(), createOffenceDecisionInformation(randomUUID(), NO_VERDICT), randomUUID(), pressRestriction);
    }

    private Adjourn adjourn(final PressRestriction pressRestriction) {
        return new Adjourn(randomUUID(), asList(createOffenceDecisionInformation(randomUUID(), NO_VERDICT)), "Not enough documents for decision", LocalDate.now(), pressRestriction);
    }

    private NoSeparatePenalty noSeparatePenalty(final PressRestriction pressRestriction) {
        return NoSeparatePenalty.createNoSeparatePenalty(randomUUID(), createOffenceDecisionInformation(randomUUID(), NO_VERDICT), false, false, pressRestriction);
    }

    private SetAside setAside(final PressRestriction pressRestriction) {
        return new SetAside(randomUUID(), asList(createOffenceDecisionInformation(randomUUID(), NO_VERDICT)), pressRestriction);
    }

    private SetAside setAside(final UUID offenceId1, final UUID offenceId2, final PressRestriction pressRestriction) {
        return new SetAside(randomUUID(),
                asList(
                        createOffenceDecisionInformation(offenceId1, NO_VERDICT),
                        createOffenceDecisionInformation(offenceId2, NO_VERDICT)
                ),
                pressRestriction);
    }

    private ReferredToOpenCourt referredToOpenCourt(final PressRestriction pressRestriction) {
        return new ReferredToOpenCourt(randomUUID(), asList(createOffenceDecisionInformation(randomUUID(), PROVED_SJP)),
                "Listing notes", 10, null, null, null, pressRestriction);
    }

    private ReferredForFutureSJPSession referredForFutureSJPSession(final UUID offenceId1,
                                                                    final UUID offenceId2,
                                                                    final PressRestriction pressRestriction) {
        return new ReferredForFutureSJPSession(
                randomUUID(),
                asList(
                        createOffenceDecisionInformation(offenceId1, PROVED_SJP),
                        createOffenceDecisionInformation(offenceId2, PROVED_SJP)
                ),
                pressRestriction);
    }

    private Optional<uk.gov.moj.cpp.sjp.persistence.entity.PressRestriction> toPressRestrictionEntity(final PressRestriction pressRestriction) {
        return isNull(pressRestriction) ?
                Optional.empty() :
                Optional.of(new uk.gov.moj.cpp.sjp.persistence.entity.PressRestriction(
                        pressRestriction.getName(),
                        pressRestriction.getRequested())
                );
    }

}
