package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.ADJOURNMENT;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.DECISION;
import static uk.gov.justice.json.schemas.domains.sjp.NoteType.LISTING;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.DecisionBuilder.decisionBuilder;
import static uk.gov.moj.cpp.sjp.domain.decision.Discharge.createDischarge;
import static uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty.createFinancialPenalty;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.ATTACH_TO_EARNINGS;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.NO_DISABILITY_NEEDS;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.disabilityNeedsOf;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static java.math.BigDecimal.valueOf;

import java.util.Arrays;
import uk.gov.justice.core.courts.DelegatedPowers;
import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.domains.sjp.events.CaseNoteAdded;
import uk.gov.moj.cpp.sjp.domain.AOCPCost;
import uk.gov.moj.cpp.sjp.domain.AOCPCostDefendant;
import uk.gov.moj.cpp.sjp.domain.AOCPCostOffence;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.AocpDecision;
import uk.gov.moj.cpp.sjp.domain.decision.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.decision.Decision;
import uk.gov.moj.cpp.sjp.domain.decision.Defendant;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.decision.SessionCourt;
import uk.gov.moj.cpp.sjp.domain.decision.SetAside;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.LumpSum;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.domain.testutils.builders.AdjournBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.builders.DismissBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.builders.FinancialPenaltyBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.builders.ReferForCourtHearingBuilder;
import uk.gov.moj.cpp.sjp.domain.testutils.builders.WithdrawBuilder;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.AocpPleasSet;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearingV2;
import uk.gov.moj.cpp.sjp.event.DefendantAocpResponseTimerExpired;
import uk.gov.moj.cpp.sjp.event.decision.DecisionRejected;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSetAside;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSetAsideReset;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseDecisionHandlerTest {

    private final UUID referralReasonId = randomUUID();
    private final UUID decisionId = randomUUID();
    private final UUID decisionId2 = randomUUID();
    private final UUID sessionId = randomUUID();
    private final UUID sessionId2 = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID legalAdviserId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final String defendantFirstName = "John";
    private final String urn = "TFL12345567";
    private final String defendantLastName = "Smith";
    private final UUID offenceId1 = randomUUID();
    private final UUID offenceId2 = randomUUID();
    private final UUID offenceId3 = randomUUID();
    private final ZonedDateTime savedAt = now();
    private final String note = "wrongly convicted";
    private final UUID withdrawalReasonId1 = randomUUID();
    private final UUID withdrawalReasonId2 = randomUUID();
    private final String adjournmentReason = "Not enough documents for decision";
    private final User legalAdviser = new User("John", "Smith", legalAdviserId);
    private CaseAggregateState caseAggregateState;
    private Session session;
    private final String courtHouseCode = "1008";
    private final String courtHouseName = "Test court";
    private final String localJusticeAreaNationalCode = "1009";
    private final String disabilityNeeds = "Disability_needs";
    private final Optional<DelegatedPowers> legalAdviserMagistrate = Optional.of(DelegatedPowers.delegatedPowers().withFirstName("Erica").withLastName("Wilson").withUserId(randomUUID()).build());

    @BeforeEach
    public void onceBeforeEachTest() {
        caseAggregateState = new CaseAggregateState();
        session = new Session();
        session.startMagistrateSession(sessionId, legalAdviserId, courtHouseCode, courtHouseName,
                localJusticeAreaNationalCode, now(), "magistrate name", legalAdviserMagistrate, Arrays.asList("TFL", "DVL"));

    }

    private List<OffenceDecisionInformation> createOffenceDecisionInformationList(final UUID offenceId, final VerdictType verdict) {
        return asList(createOffenceDecisionInformation(offenceId, verdict));
    }

    @Test
    public void shouldAcceptDecisionWithAllOffencesWithdrawn() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId2, NO_VERDICT), withdrawalReasonId2));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAccepted(decision, eventList);
    }

    @Test
    public void shouldAcceptDecisionWithAllOffencesAdjournedPostConvictionOfFoundGuiltyVerdict() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final Adjourn adjourn = new Adjourn(randomUUID(), asList(
                createOffenceDecisionInformation(offenceId1, FOUND_GUILTY),
                createOffenceDecisionInformation(offenceId2, FOUND_GUILTY)),
                adjournmentReason, adjournedTo);
        adjourn.setConvictionCourt(new SessionCourt("B01OK", "2572"));
        final List<OffenceDecision> offenceDecisions = newArrayList(adjourn);

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(offenceDecisions, eventList, adjournedTo, true);
    }

    @Test
    public void shouldAcceptDecisionWithAllOffencesAdjournedPostConvictionOfProvedSjpVerdict() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, PROVED_SJP),
                        createOffenceDecisionInformation(offenceId2, PROVED_SJP)),
                        adjournmentReason, adjournedTo));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(offenceDecisions, eventList, adjournedTo, true);
    }

    @Test
    public void shouldAcceptDecisionAdjournedWithPostConvictionOfBothVerdictsProvedSjpAndFoundGuiltyCombined() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, FOUND_GUILTY),
                        createOffenceDecisionInformation(offenceId2, PROVED_SJP)),
                        adjournmentReason, adjournedTo));
        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(offenceDecisions, eventList, adjournedTo, true);
    }

    @Test
    public void shouldAcceptDecisionWithAllOffencesAdjournedPreConviction() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, NO_VERDICT),
                        createOffenceDecisionInformation(offenceId2, NO_VERDICT)),
                        adjournmentReason, adjournedTo));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(offenceDecisions, eventList, adjournedTo, false);
    }

    @Test
    public void shouldAcceptDecisionWithAllOffencesAdjournedWithConviction() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, FOUND_GUILTY),
                        createOffenceDecisionInformation(offenceId2, FOUND_GUILTY)),
                        adjournmentReason, adjournedTo));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(offenceDecisions, eventList, adjournedTo, true);
    }

    @Test
    public void shouldAcceptAdjournPostConviction() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, FOUND_GUILTY),
                        createOffenceDecisionInformation(offenceId2, FOUND_GUILTY)),
                        adjournmentReason, adjournedTo));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);
        CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        caseAggregateState.updateOffenceDecisions(offenceDecisions, sessionId);
        caseAggregateState.updateOffenceConvictionDetails(savedAt, offenceDecisions, null);

        final List<OffenceDecision> offenceDecisions2 = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, null),
                        createOffenceDecisionInformation(offenceId2, null)),
                        adjournmentReason, adjournedTo));

        final Decision decision2 = new Decision(decisionId2, sessionId2, caseId, note, savedAt, legalAdviser, offenceDecisions2, null);
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision2, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(decisionId2, sessionId2, offenceDecisions2, eventList, adjournedTo, true);
        thenTheOffenceDecisionsReflectTheConvictionDate(offenceDecisions2, Adjourn.class, savedAt.toLocalDate());

    }

    @Test
    public void shouldAcceptAdjournWithConvictionAndReferToCourt() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        caseAggregateState.setPleas(asList(
                new Plea(defendantId, offenceId1, PleaType.GUILTY),
                new Plea(defendantId, offenceId2, PleaType.GUILTY)
        ));
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, FOUND_GUILTY),
                        createOffenceDecisionInformation(offenceId2, FOUND_GUILTY)),
                        adjournmentReason, adjournedTo));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);
        CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        caseAggregateState.updateOffenceDecisions(offenceDecisions, sessionId);
        caseAggregateState.updateOffenceConvictionDetails(savedAt, offenceDecisions, null);

        final DefendantCourtOptions courtOptions =
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("EN", true),
                        false,
                        NO_DISABILITY_NEEDS);

        final List<OffenceDecision> offenceDecisions2 = newArrayList(
                new ReferForCourtHearing(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, null),
                        createOffenceDecisionInformation(offenceId2, null)),
                        referralReasonId, "note", 0, courtOptions, null));

        final Decision decision2 = new Decision(decisionId2, sessionId2, caseId, note, savedAt, legalAdviser, offenceDecisions2, null);
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision2, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseReferForCourtHearingRecordedEvent(decisionId2, sessionId2, offenceDecisions2, eventList);
        thenTheOffenceDecisionsReflectTheConvictionDate(offenceDecisions2, ReferForCourtHearing.class, savedAt.toLocalDate());

    }

    @Test
    public void shouldRejectWithdrawDismissDecisionsWithPreviousConviction() {
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, FOUND_GUILTY),
                        createOffenceDecisionInformation(offenceId2, FOUND_GUILTY)),
                        adjournmentReason, adjournedTo));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);
        CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        caseAggregateState.updateOffenceDecisions(offenceDecisions, sessionId);
        caseAggregateState.updateOffenceConvictionDetails(savedAt, offenceDecisions, null);

        final List<OffenceDecision> offenceDecisions2 = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new Dismiss(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_NOT_GUILTY), null)
        );

        final Decision decision2 = new Decision(decisionId2, sessionId2, caseId, note, savedAt, legalAdviser, offenceDecisions2, null);
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision2, caseAggregateState, session);

        final List<String> expectedRejectionReasons = asList(
                format("offence %s : WITHDRAW or DISMISS can't be used on an offence decision with a previous conviction", offenceId1.toString()),
                format("offence %s : WITHDRAW or DISMISS can't be used on an offence decision with a previous conviction", offenceId2.toString())
        );
        thenTheDecisionIsRejected(decision2, expectedRejectionReasons, eventStream);
    }

    @Test
    public void shouldRejectReferForCourtHearingWithoutVerdictWithoutPreviousConviction() {
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final DefendantCourtOptions courtOptions =
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("EN", true),
                        false,
                        NO_DISABILITY_NEEDS);

        final List<OffenceDecision> offenceDecisions = newArrayList(
                new ReferForCourtHearing(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, null),
                        createOffenceDecisionInformation(offenceId2, null)),
                        referralReasonId, "note", 0, courtOptions, null));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<String> expectedRejectionReasons = asList(
                format("offence %s : can't have an offence without verdict if it wasn't previously convicted", offenceId1.toString()),
                format("offence %s : can't have an offence without verdict if it wasn't previously convicted", offenceId2.toString())
        );
        thenTheDecisionIsRejected(decision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void shouldAcceptDecisionWithAllOffencesDismissed() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Dismiss(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_NOT_GUILTY), null),
                new Dismiss(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_NOT_GUILTY), null));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAccepted(decision, eventList);
    }

    @Test
    public void shouldAcceptDecisionWhenOffenceWasPreviouslyAdjourned() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final List<OffenceDecision> previousOffenceDecisions = newArrayList(new Adjourn(randomUUID(), newArrayList(createOffenceDecisionInformationList(offenceId1, NO_VERDICT)), adjournmentReason, adjournedTo),
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId2, NO_VERDICT), withdrawalReasonId1));
        caseAggregateState.updateOffenceDecisions(previousOffenceDecisions, sessionId);

        final ArrayList<OffenceDecision> currentOffenceDecisions = newArrayList(new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1));
        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, currentOffenceDecisions, null);
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAccepted(decision, eventList);
    }

    @Test
    public void shouldAcceptDecisionWithOffencesWithdrawnAndAdjourned() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final LocalDate adjournedTo = LocalDate.now().plusDays(10);

        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new Adjourn(randomUUID(), createOffenceDecisionInformationList(offenceId2, NO_VERDICT), adjournmentReason, adjournedTo));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(offenceDecisions, eventList, adjournedTo, false);
    }

    @Test
    public void shouldAcceptDecisionWithOffencesWithdrawnAndAdjournedAndDismissed() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2, offenceId3), legalAdviserId);

        final LocalDate adjournedTo = LocalDate.now().plusDays(10);

        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new Adjourn(randomUUID(), createOffenceDecisionInformationList(offenceId2, NO_VERDICT), adjournmentReason, adjournedTo),
                new Dismiss(randomUUID(), createOffenceDecisionInformation(offenceId3, FOUND_NOT_GUILTY), null));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(offenceDecisions, eventList, adjournedTo, false);
    }

    @Test
    public void shouldAcceptDecisionWithOffencesWithdrawnAndReferForCourtHearing() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final DefendantCourtOptions courtOptions =
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("EN", true),
                        false,
                        NO_DISABILITY_NEEDS);

        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new ReferForCourtHearing(randomUUID(), createOffenceDecisionInformationList(offenceId2, PROVED_SJP),
                        referralReasonId, "note", 0, courtOptions, null));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseReferForCourtHearingRecordedEvent(offenceDecisions, eventList);
    }

    @Test
    public void shouldAcceptDecisionWithOffencesDischarged() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final List<OffenceDecision> offenceDecisions = newArrayList(
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), DischargeType.CONDITIONAL, null, new BigDecimal(500), null, true, null, null),
                createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, null, "Insufficient means", false, null, null));

        final Defendant defendant = new Defendant(new CourtDetails("1080", "Bedfordshire Magistrates' Court"));
        final FinancialImposition financialImposition = new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(150), null, new BigDecimal(30), null, null, true),
                new Payment(new BigDecimal(680), PAY_TO_COURT, null, null, new PaymentTerms(
                        true,
                        new LumpSum(new BigDecimal(680), 25, LocalDate.of(2019, 9, 1)),
                        null
                ), null)
        );
        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, financialImposition, defendant);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAccepted(decision, eventList);
    }

    @Test
    public void shouldAcceptDecisionWithOffencesWithdrawnAndReferForCourtHearingAndDismiss() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2, offenceId3), legalAdviserId);

        final DefendantCourtOptions courtOptions =
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("EN", true),
                        false,
                        disabilityNeedsOf(disabilityNeeds));

        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new ReferForCourtHearing(randomUUID(), createOffenceDecisionInformationList(offenceId2, PROVED_SJP),
                        referralReasonId, "note", 0, courtOptions, null),
                new Dismiss(randomUUID(), createOffenceDecisionInformation(offenceId3, FOUND_NOT_GUILTY), null));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());
        thenTheDecisionIsAcceptedAlongWithCaseReferForCourtHearingRecordedEvent(offenceDecisions, eventList);
    }

    @Test
    public void shouldRaiseRejectedEventWhenPreviousAdjournedOffenceWasNotSubmitted() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2, offenceId3), legalAdviserId);
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final List<OffenceDecision> previousOffenceDecisions = newArrayList(
                new Adjourn(randomUUID(), newArrayList(createOffenceDecisionInformation(offenceId1, NO_VERDICT), createOffenceDecisionInformation(offenceId3, NO_VERDICT)), adjournmentReason, adjournedTo),
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId2, NO_VERDICT), withdrawalReasonId1));
        caseAggregateState.updateOffenceDecisions(previousOffenceDecisions, sessionId);

        final ArrayList<OffenceDecision> currentOffenceDecisions = newArrayList(new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1));
        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, currentOffenceDecisions, null);
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<String> rejectionReason = newArrayList(format("Offence with id %s must have a decision", offenceId3));
        thenTheDecisionIsRejected(decision, rejectionReason, eventStream);
    }

    @Test
    public void shouldRaiseRejectedEventWhenSubmittedWithOffenceDoesNotBelongToTheCase() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final List<OffenceDecision> previousOffenceDecisions = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId2, NO_VERDICT), withdrawalReasonId1));
        caseAggregateState.updateOffenceDecisions(previousOffenceDecisions, sessionId);

        final ArrayList<OffenceDecision> currentOffenceDecisions = newArrayList(new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId3, NO_VERDICT), withdrawalReasonId1));
        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, currentOffenceDecisions, null);
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<String> rejectionReason = newArrayList(format("Offence with id %s does not belong to this case", offenceId3));
        thenTheDecisionIsRejected(decision, rejectionReason, eventStream);
    }

    @Test
    public void shouldRaiseDecisionRejectedEventWhenCaseIsAlreadyCompleted() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        caseAggregateState.markCaseCompleted();
        final List<OffenceDecision> offenceDecisions = newArrayList(new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId2, NO_VERDICT), withdrawalReasonId1));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        final List<String> rejectionReason = newArrayList("The case is already completed");
        thenTheDecisionIsRejected(decision, rejectionReason, eventStream);
    }

    @Test
    public void shouldRaiseDecisionRejectedEventWhenCaseIsNotAssignedToCaller() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), UUID.randomUUID());
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId2, NO_VERDICT), withdrawalReasonId1));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<String> rejectionReason = newArrayList("The case must be assigned to the caller");
        thenTheDecisionIsRejected(decision, rejectionReason, eventStream);
    }


    @Test
    public void shouldRaiseDecisionRejectedEventWhenDecisionWithPaymentTypeOfAttachedToEarningsDoesNotHaveUpdatedEmployerDetails() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final Discharge discharge1 = createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), ABSOLUTE, null, new BigDecimal(10), null, true, null, null);
        final Discharge discharge2 = createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, new BigDecimal(20), null, true, null, null);
        final List<OffenceDecision> offenceDecisions = newArrayList(discharge1, discharge2);

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser,
                offenceDecisions, new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(40), null, new BigDecimal(10), null, null, true),
                new Payment(new BigDecimal(70), ATTACH_TO_EARNINGS, null, null, null, null)
        ), null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<String> rejectionReason = newArrayList("Decision with payment type attach to earnings requires employer details");
        thenTheDecisionIsRejected(decision, rejectionReason, eventStream);
    }

    @Test
    public void shouldRaiseDecisionRejectedEventWhenDecisionWithFinancialImpositionHasCostsZeroAndNoReason() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final Discharge discharge1 = createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), ABSOLUTE, null, new BigDecimal(10), null, true, null, null);
        final Discharge discharge2 = createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, new BigDecimal(20), null, true, null, null);
        final List<OffenceDecision> offenceDecisions = newArrayList(discharge1, discharge2);

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser,
                offenceDecisions, new FinancialImposition(
                new CostsAndSurcharge(BigDecimal.ZERO, null, new BigDecimal(10), null, null, true),
                new Payment(new BigDecimal(70), PAY_TO_COURT, null, null, null, null)
        ), null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<String> rejectionReason = newArrayList("Reason for no costs is required when costs is zero");
        thenTheDecisionIsRejected(decision, rejectionReason, eventStream);
    }

    @Test
    public void shouldAllowZeroCostsDecisionWhenCaseHasNoProsecutorCosts() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        caseAggregateState.setCosts(BigDecimal.ZERO);
        final Discharge discharge1 = createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId1, FOUND_GUILTY), ABSOLUTE, null, new BigDecimal(10), null, true, null, null);
        final Discharge discharge2 = createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, new BigDecimal(20), null, true, null, null);
        final List<OffenceDecision> offenceDecisions = newArrayList(discharge1, discharge2);

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser,
                offenceDecisions, new FinancialImposition(
                new CostsAndSurcharge(BigDecimal.ZERO, null, new BigDecimal(10), null, null, true),
                new Payment(new BigDecimal(70), PAY_TO_COURT, null, null, null, null)
        ), null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        thenTheDecisionIsAccepted(decision, eventStream.collect(toList()));
    }


    @Test
    public void shouldRaiseDecisionRejectedEventWhenOffenceHasMoreThanOneDecision() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1), legalAdviserId);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId2));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);
        final List<String> rejectionReason = newArrayList(format("Offence with id %s has more than 1 decision", offenceId1));

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        thenTheDecisionIsRejected(decision, rejectionReason, eventStream);
    }

    @Test
    public void shouldRaiseDecisionRejectedEventNotAllOffenceHaveDecision() {
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1));

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);
        final List<String> rejectionReason = newArrayList(format("Offence with id %s must have a decision", offenceId2));

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        thenTheDecisionIsRejected(decision, rejectionReason, eventStream);
    }

    @Test
    public void shouldRaiseDecisionRejectedEventWhenOffenceAlreadyHasFinalDecision() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1), legalAdviserId);
        final List<OffenceDecision> offenceDecisions = newArrayList(new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1));
        caseAggregateState.updateOffenceDecisions(offenceDecisions, sessionId);

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);
        final List<String> rejectionReason = newArrayList(format("Offence %s already has a final decision", offenceId1));

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        thenTheDecisionIsRejected(decision, rejectionReason, eventStream);
    }

    @Test
    public void shouldRaiseDecisionRejectedEventWhenInvalidReferForCourtHearingAndAdjournDecisionCombination() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final LocalDate adjournedTo = LocalDate.now().plusDays(10);

        final DefendantCourtOptions courtOptions =
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("EN", true),
                        false,
                        disabilityNeedsOf(disabilityNeeds));

        final ReferForCourtHearing referForCourtHearing = new ReferForCourtHearing(randomUUID(), createOffenceDecisionInformationList(offenceId1, NO_VERDICT),
                referralReasonId, "note", 0, courtOptions, null);
        final Adjourn adjourn = new Adjourn(randomUUID(), createOffenceDecisionInformationList(offenceId2, NO_VERDICT), adjournmentReason, adjournedTo);

        final List<OffenceDecision> offenceDecisions = newArrayList(referForCourtHearing, adjourn);

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);
        final List<String> expectedRejectionReasons = newArrayList("REFER_FOR_COURT_HEARING decision can not be saved with decision(s) ADJOURN");

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        thenTheDecisionIsRejected(decision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void shouldRaiseDecisionRejectedEventWhenInvalidReferForCourtHearingAndDischargeCombination() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final DefendantCourtOptions courtOptions =
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("EN", true),
                        false,
                        disabilityNeedsOf(disabilityNeeds));

        final ReferForCourtHearing referForCourtHearing = new ReferForCourtHearing(randomUUID(), createOffenceDecisionInformationList(offenceId1, NO_VERDICT),
                referralReasonId, "note", 0, courtOptions, null);
        final Discharge discharge = createDischarge(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), ABSOLUTE, null, new BigDecimal(20), null, true, null, null);

        final List<OffenceDecision> offenceDecisions = newArrayList(referForCourtHearing, discharge);

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser,
                offenceDecisions, new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(40), null, new BigDecimal(10), null, null, true),
                new Payment(new BigDecimal(70), PAY_TO_COURT, null, null, null, null)
        ), null);
        final List<String> expectedRejectionReasons = newArrayList("REFER_FOR_COURT_HEARING decision can not be saved with decision(s) DISCHARGE");

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        thenTheDecisionIsRejected(decision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void shouldRaiseDecisionRejectedEventWhenInvalidAdjournAndFinancialPenaltyCombination() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final Adjourn adjourn = new Adjourn(randomUUID(), createOffenceDecisionInformationList(offenceId1, NO_VERDICT), adjournmentReason, LocalDate.now().plusDays(14));
        final FinancialPenalty financialPenalty = createFinancialPenalty(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), new BigDecimal(200), new BigDecimal(40), null, true, null, null, null);

        final List<OffenceDecision> offenceDecisions = newArrayList(adjourn, financialPenalty);

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser,
                offenceDecisions, new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(40), null, new BigDecimal(10), null, null, true),
                new Payment(new BigDecimal(70), PAY_TO_COURT, null, null, null, null)
        ), null);
        final List<String> expectedRejectionReasons = newArrayList("ADJOURN decision can not be saved with decision(s) FINANCIAL_PENALTY");

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        thenTheDecisionIsRejected(decision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void shouldRaiseDecisionRejectedEventWhenInvalidAdjournPreAndPostAreCombined() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);
        final Adjourn adjourn = new Adjourn(randomUUID(), createOffenceDecisionInformationList(offenceId1, NO_VERDICT), adjournmentReason, LocalDate.now().plusDays(14));
        final FinancialPenalty financialPenalty = createFinancialPenalty(randomUUID(), createOffenceDecisionInformation(offenceId2, FOUND_GUILTY), new BigDecimal(200), new BigDecimal(40), null, true, null, null, null);
        final List<OffenceDecision> offenceDecisions = newArrayList(adjourn, financialPenalty);
        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<String> expectedRejectionReasons = newArrayList("ADJOURN decision can not be saved with decision(s) FINANCIAL_PENALTY");
        thenTheDecisionIsRejected(decision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void shouldRaiseDecisionRejectedEventWhenAdjournPreAndPostConvictionDecisionsAreCombined() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2, offenceId3), legalAdviserId);
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, NO_VERDICT),
                        createOffenceDecisionInformation(offenceId2, FOUND_GUILTY),
                        createOffenceDecisionInformation(offenceId3, PROVED_SJP)),
                        adjournmentReason, adjournedTo));
        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<String> expectedRejectionReasons = newArrayList("ADJOURN decisions with pre and post convictions can not be combined");
        thenTheDecisionIsRejected(decision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void should_Raise_Decision_Rejected_Event_For_Discharge_And_Financial_Penalty_Decision_When_Verdict_Is_Invalid() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final FinancialPenalty financialPenalty1 = decisionBuilder(randomUUID()).offenceId(offenceId1).verdict(NO_VERDICT).build(FinancialPenalty.class);
        final Discharge discharge = decisionBuilder(randomUUID()).offenceId(offenceId2).verdict(NO_VERDICT).build(Discharge.class);

        final List<OffenceDecision> offenceDecisions = newArrayList(financialPenalty1, discharge);

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);
        final List<String> expectedRejectionReasons = newArrayList("Decisions of type Financial Penalty and Discharge's verdict type can only be either FOUND_GUILTY or PROVED_SJP");

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        thenTheDecisionIsRejected(decision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void should_Raise_Decision_Rejected_Event_For_Withdraw_Decision_When_Verdict_Is_Invalid() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final Withdraw withdraw1 = decisionBuilder(randomUUID()).offenceId(offenceId1).verdict(FOUND_GUILTY).build(Withdraw.class);
        final Withdraw withdraw2 = decisionBuilder(randomUUID()).offenceId(offenceId2).verdict(NO_VERDICT).build(Withdraw.class);

        final List<OffenceDecision> offenceDecisions = newArrayList(withdraw1, withdraw2);

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);
        final List<String> expectedRejectionReasons = newArrayList("Decisions of type Withdraw's verdict type can only be NO_VERDICT");

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        thenTheDecisionIsRejected(decision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void should_Raise_Decision_Rejected_Event_For_Dismiss_Decision_When_Verdict_Is_Invalid() {

        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final Dismiss dismiss1 = decisionBuilder(randomUUID()).offenceId(offenceId1).verdict(FOUND_NOT_GUILTY).build(Dismiss.class);
        final Dismiss dismiss2 = decisionBuilder(randomUUID()).offenceId(offenceId2).verdict(NO_VERDICT).build(Dismiss.class);

        final List<OffenceDecision> offenceDecisions = newArrayList(dismiss1, dismiss2);

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);
        final List<String> expectedRejectionReasons = newArrayList("Decisions of type Dismiss's verdict type can only be FOUND_NOT_GUILTY");

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);
        thenTheDecisionIsRejected(decision, expectedRejectionReasons, eventStream);
    }

    @Test
    public void shouldRejectPressRestrictionOnNonPressRestrictableOffence() {
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults().pressRestriction("A Name").build();
        final Dismiss dismiss = DismissBuilder.withDefaults().pressRestriction("A Name").build();
        givenCaseExistsWithMultipleOffences(newHashSet(financialPenalty.getId(), dismiss.getId()), legalAdviserId);
        final List<OffenceDecision> offenceDecisions = newArrayList(financialPenalty, dismiss);
        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<String> expected = newArrayList(
                "Press restriction cannot be applied to non-press-restrictable offence: " + financialPenalty.getId(),
                "Press restriction cannot be applied to non-press-restrictable offence: " + dismiss.getId()
        );
        thenTheDecisionIsRejected(decision, expected, eventStream);
    }

    @Test
    public void shouldRejectRevokedRestrictionForOffencesWithoutPreviousRestrictionRequested() {
        final FinancialPenaltyBuilder financialPenaltyBuilder = FinancialPenaltyBuilder.withDefaults().pressRestrictionRevoked();
        final DismissBuilder dismissBuilder = DismissBuilder.withDefaults().pressRestrictionRevoked();
        final Withdraw withdraw = WithdrawBuilder.withDefaults().build();
        final FinancialPenalty financialPenalty = financialPenaltyBuilder.build();
        final Dismiss dismiss = dismissBuilder.build();
        final List<OffenceDecision> offenceDecisions = newArrayList(financialPenalty, dismiss, withdraw);
        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);
        givenCaseExistsWithMultipleOffences(newHashSet(financialPenalty.getId(), dismiss.getId(), withdraw.getId()), legalAdviserId);
        caseAggregateState.markOffenceAsPressRestrictable(financialPenalty.getId());
        caseAggregateState.markOffenceAsPressRestrictable(dismiss.getId());

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<String> expected = newArrayList(
                "Press restriction cannot be revoked on offence that has no previous press restriction requested. Failed offenceId: " + financialPenalty.getId(),
                "Press restriction cannot be revoked on offence that has no previous press restriction requested. Failed offenceId: " + dismiss.getId()
        );
        thenTheDecisionIsRejected(decision, expected, eventStream);
    }

    @Test
    public void shouldRejectNullRestrictionForOffencesWithPreviousRestrictionRequested() {
        // Given a first decision without press restriction
        final Adjourn adjourn = AdjournBuilder.withDefaults()
                .addOffenceDecisionInformation(offenceId1, NO_VERDICT)
                .addOffenceDecisionInformation(offenceId2, NO_VERDICT)
                .addOffenceDecisionInformation(offenceId3, NO_VERDICT)
                .pressRestriction("Robert")
                .build();
        caseAggregateState.markOffenceAsPressRestrictable(offenceId1);
        caseAggregateState.markOffenceAsPressRestrictable(offenceId2);
        caseAggregateState.updateOffenceDecisions(newArrayList(adjourn), sessionId);
        caseAggregateState.addOffenceIdsForDefendant(defendantId, newHashSet(offenceId1, offenceId2, offenceId3));

        // And a second decision with revoked press restriction
        final FinancialPenalty financialPenalty = FinancialPenaltyBuilder.withDefaults().id(offenceId1).build();
        final Dismiss dismiss = DismissBuilder.withDefaults().pressRestrictionRevoked().id(offenceId2).build();
        final Withdraw withdraw = WithdrawBuilder.withDefaults().id(offenceId3).build();
        final List<OffenceDecision> offenceDecisions = newArrayList(financialPenalty, dismiss, withdraw);

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        // When
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        // Then
        final List<String> expected = newArrayList(
                String.format("Expected to find press restriction for offence %s but found none", financialPenalty.getId().toString())
        );
        thenTheDecisionIsRejected(decision, expected, eventStream);
    }

    @Test
    public void shouldAcceptRevokedRestrictionForOffencesWithPreviousRestrictionRequestedWhenAdjourning() {
        // Given a first decision without press restriction
        final Adjourn adjourn = AdjournBuilder.withDefaults()
                .addOffenceDecisionInformation(offenceId1, NO_VERDICT)
                .addOffenceDecisionInformation(offenceId2, NO_VERDICT)
                .addOffenceDecisionInformation(offenceId3, NO_VERDICT)
                .pressRestriction("Robert")
                .build();
        caseAggregateState.markOffenceAsPressRestrictable(offenceId1);
        caseAggregateState.markOffenceAsPressRestrictable(offenceId2);
        caseAggregateState.updateOffenceDecisions(newArrayList(adjourn), sessionId);
        caseAggregateState.addOffenceIdsForDefendant(defendantId, newHashSet(offenceId1, offenceId2, offenceId3));

        // And a second decision with revoked press restriction
        final Adjourn adjourn2 = AdjournBuilder.withDefaults()
                .addOffenceDecisionInformation(offenceId1, NO_VERDICT)
                .addOffenceDecisionInformation(offenceId2, NO_VERDICT)
                .addOffenceDecisionInformation(offenceId3, NO_VERDICT)
                .reason(adjournmentReason)
                .pressRestrictionRevoked()
                .build();

        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, newArrayList(adjourn2), null);
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2, offenceId3), legalAdviserId);

        // When
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        // Then
        thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(newArrayList(adjourn2), eventStream.collect(toList()), adjourn2.getAdjournTo(), false);
    }

    @Test
    public void shouldBypassPressRestrictionValidationForAdjournDecision() {
        final Adjourn adjourn = AdjournBuilder.withDefaults()
                .addOffenceDecisionInformation(NO_VERDICT)
                .addOffenceDecisionInformation(NO_VERDICT)
                .reason(adjournmentReason)
                .pressRestriction("A random name")
                .build();
        givenCaseExistsWithMultipleOffences(newHashSet(adjourn.getOffenceIds()), legalAdviserId);
        caseAggregateState.markOffenceAsPressRestrictable(adjourn.getOffenceIds().get(0));
        final List<OffenceDecision> offenceDecisions = newArrayList(adjourn);
        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(offenceDecisions, eventStream.collect(toList()), adjourn.getAdjournTo(), false);
    }

    @Test
    public void shouldBypassPressRestrictionValidationForReferForCourtHearingDecisionWithNextHearing() {
        final ReferForCourtHearing referForCourtHearing = ReferForCourtHearingBuilder.withDefaults()
                .addOffenceDecisionInformation(NO_VERDICT)
                .addOffenceDecisionInformation(NO_VERDICT)
                .pressRestriction("A random name")
                .withNextHearing(NextHearing.nextHearing().build())
                .build();

        givenCaseExistsWithMultipleOffences(newHashSet(referForCourtHearing.getOffenceIds()), legalAdviserId);
        final List<OffenceDecision> offenceDecisions = newArrayList(referForCourtHearing);
        final Decision decision = new Decision(decisionId, sessionId, caseId, note, savedAt, legalAdviser, offenceDecisions, null);
        caseAggregateState.markOffenceAsPressRestrictable(referForCourtHearing.getOffenceIds().get(0));

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        final List<Object> actualEvents = eventStream.collect(toList());
        thenTheDecisionIsNotRejected(actualEvents);
        assertThat(actualEvents.get(3).getClass(), equalTo(CaseReferredForCourtHearingV2.class));
    }

    @Test
    public void shouldNotUnAssignWhenTheCurrentDecisionIsSetAside() {
        // given
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();

        List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, FOUND_GUILTY),
                        createOffenceDecisionInformation(offenceId2, FOUND_GUILTY)),
                        "adjourn reason ", adjournedTo));

        caseAggregateState.updateOffenceConvictionDetails(zonedDateTime, offenceDecisions, null);

        offenceDecisions = newArrayList(
                new SetAside(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, null),
                        createOffenceDecisionInformation(offenceId2, null))));

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);

        // when
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        // then
        assertThat(eventStream.allMatch(e -> e instanceof CaseUnassigned == false), is(true));
    }

    @Test
    public void shouldRaiseADecisionSetAsideEventWhenTheCurrentDecisionIsSetAside() {
        // given
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, FOUND_GUILTY),
                        createOffenceDecisionInformation(offenceId2, FOUND_GUILTY)),
                        "adjourn reason ", adjournedTo));
        caseAggregateState.updateOffenceConvictionDetails(zonedDateTime, offenceDecisions, null);


        offenceDecisions = newArrayList(
                new SetAside(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, null),
                        createOffenceDecisionInformation(offenceId2, null))));

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);

        // when
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        // then
        assertThat(eventStream.anyMatch(e -> e instanceof DecisionSetAside), is(true));
    }

    @Test
    public void shouldRaiseADecisionSetAsideResetEventWhenThePreviousIsSetAside() {
        // given
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        caseAggregateState.setSetAside(true);

        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, FOUND_GUILTY),
                        createOffenceDecisionInformation(offenceId2, FOUND_GUILTY)),
                        "adjourn reason ", adjournedTo));

        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);

        // when
        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        // then
        assertThat(eventStream.anyMatch(e -> e instanceof DecisionSetAsideReset), is(true));
    }

    @Test
    public void shouldNotRaiseADecisionRejectedEventFinalDecisionPresentWhenATerminalDecisionIsSetAside() {
        // given
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        final Dismiss dismiss = decisionBuilder(randomUUID()).offenceId(offenceId1).verdict(FOUND_NOT_GUILTY).build(Dismiss.class);
        List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(createOffenceDecisionInformation(offenceId2, FOUND_GUILTY)),
                        "adjourn reason ", adjournedTo), dismiss);

        final Decision decision1 = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);
        Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision1, caseAggregateState, session);

        caseAggregateState.updateOffenceConvictionDetails(zonedDateTime, offenceDecisions, null);
        caseAggregateState.updateOffenceDecisions(offenceDecisions, sessionId);

        offenceDecisions = newArrayList(
                new SetAside(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, null),
                        createOffenceDecisionInformation(offenceId2, null))));
        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);

        caseAggregateState.updateOffenceConvictionDetails(zonedDateTime, offenceDecisions, null);
        caseAggregateState.updateOffenceDecisions(offenceDecisions, sessionId);

        // when
        final List eventList = (List) CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session).collect(Collectors.toList());

        // then
        assertThat(eventList.stream()
                .filter(e -> e instanceof DecisionRejected)
                .anyMatch(e -> ((DecisionRejected) e)
                        .getRejectionReasons()
                        .contains(String.format("Offence %s already has a final decision", offenceId1))), is(false));

        assertThat(eventList.stream()
                .filter(e -> e instanceof DecisionRejected)
                .anyMatch(e -> ((DecisionRejected) e)
                        .getRejectionReasons()
                        .contains(format("offence %s : can't have an offence without verdict if it wasn't previously convicted", offenceId1))), is(false));


    }


    @Test
    public void shouldRaiseADecisionRejectedEventWhenAThereIsAnotherDecisionAlongWithSetAside() {
        // given
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final ZonedDateTime zonedDateTime = ZonedDateTime.now();
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final Dismiss dismiss = decisionBuilder(randomUUID()).offenceId(offenceId1).verdict(FOUND_NOT_GUILTY).build(Dismiss.class);
        List<OffenceDecision> offenceDecisions = newArrayList(dismiss,
                new SetAside(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, null),
                        createOffenceDecisionInformation(offenceId2, null))));
        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);

        // when
        final Stream eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        // then
        assertThat(eventStream
                .filter(e -> e instanceof DecisionRejected)
                .anyMatch(e -> ((DecisionRejected) e)
                        .getRejectionReasons()
                        .contains(("Along with set-aside not other decisions are allowed"))), is(true));
    }

    @Test
    public void shouldRaiseADecisionRejectedEventWhenAThereIsAreMultipleSetAsideDecisions() {
        // given
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final ZonedDateTime zonedDateTime = ZonedDateTime.now();
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final Dismiss dismiss = decisionBuilder(randomUUID()).offenceId(offenceId1).verdict(FOUND_NOT_GUILTY).build(Dismiss.class);
        final List<OffenceDecision> offenceDecisions = newArrayList(new SetAside(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, null))),
                new SetAside(randomUUID(), asList(createOffenceDecisionInformation(offenceId2, null))));
        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);

        // when
        final Stream eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        // then
        assertThat(eventStream
                .filter(e -> e instanceof DecisionRejected)
                .anyMatch(e -> ((DecisionRejected) e)
                        .getRejectionReasons()
                        .contains("Only one set-aside decision can be made")), is(true));
    }

    @Test
    public void shouldRaiseADecisionRejectedEventForVerdictTypeWhenATerminalDecisionIsSetAside() {
        // given
        givenCaseExistsWithMultipleOffences(newHashSet(offenceId1, offenceId2), legalAdviserId);

        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        ZonedDateTime zonedDateTime = ZonedDateTime.now();
        final Dismiss dismiss = decisionBuilder(randomUUID()).offenceId(offenceId1).verdict(FOUND_NOT_GUILTY).build(Dismiss.class);
        List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(createOffenceDecisionInformation(offenceId2, FOUND_GUILTY)),
                        "adjourn reason ", adjournedTo), dismiss);

        final Decision decision1 = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);
        Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision1, caseAggregateState, session);

        caseAggregateState.updateOffenceConvictionDetails(zonedDateTime, offenceDecisions, null);
        caseAggregateState.updateOffenceDecisions(offenceDecisions, sessionId);

        offenceDecisions = newArrayList(
                new SetAside(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId1, null),
                        createOffenceDecisionInformation(offenceId2, FOUND_GUILTY))));
        final Decision decision = new Decision(decisionId, sessionId, caseId, this.note, savedAt, legalAdviser, offenceDecisions, null);

        caseAggregateState.updateOffenceConvictionDetails(zonedDateTime, offenceDecisions, null);
        caseAggregateState.updateOffenceDecisions(offenceDecisions, sessionId);

        // when
        eventStream = CaseDecisionHandler.INSTANCE.saveDecision(decision, caseAggregateState, session);

        // then
        assertThat(eventStream
                .filter(e -> e instanceof DecisionRejected)
                .anyMatch(e -> ((DecisionRejected) e)
                        .getRejectionReasons()
                        .contains(String.format("Decisions of type %s's verdict type can only be %s", "SetAside", null))), is(true));
    }

    @Test
    public void shouldSaveAocpDecision() {
        caseAggregateState.setCaseId(caseId);
        caseAggregateState.setDefendantId(defendantId);
        caseAggregateState.setAocpTotalCost(valueOf(200.00));

        CourtDetails courtDetails = new CourtDetails("12345", "LavenderHill");
        Defendant defendant = new Defendant(courtDetails);

        AocpDecision aocpDecision = new AocpDecision(decisionId, sessionId, caseId, legalAdviser, defendant);

        final AOCPCostOffence offence = new AOCPCostOffence(offenceId1, valueOf(2.5), valueOf(100), true, true);
        final AOCPCostDefendant aocpDefendant = new AOCPCostDefendant(defendantId, asList(offence));
        AOCPCost aocpCost = new AOCPCost(caseId, new BigDecimal(5.5), aocpDefendant);
        caseAggregateState.addAOCPCost(caseId, aocpCost);

        session = new Session();
        session.startAocpSession(sessionId, legalAdviserId, "B52CM00", "Bristol Magistrates' Court", "1450", now(), Arrays.asList("TFL", "DVL"));
        caseAggregateState.addOffenceIdsForDefendant(defendantId, new HashSet<>(asList(offenceId1)));

        final Stream<Object> eventStream = CaseDecisionHandler.INSTANCE.expireAocpResponseTimerAndSaveDecision(aocpDecision, caseAggregateState, session);
        final List<Object> eventList = eventStream.collect(toList());


        assertThat(eventList, hasItem(allOf(Matchers.instanceOf(DefendantAocpResponseTimerExpired.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId))
        )));

        assertThat(eventList, hasItem(allOf(Matchers.instanceOf(DecisionSaved.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("decisionId", is(decisionId)),
                Matchers.<CaseNoteAdded>hasProperty("sessionId", is(sessionId))
        )));

        assertThat(eventList, hasItem(allOf(Matchers.instanceOf(AocpPleasSet.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("pleas")
        )));

        assertThat(eventList, hasItem(allOf(Matchers.instanceOf(CaseCompleted.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId))
        )));
    }


    private void givenCaseExistsWithMultipleOffences(final HashSet<UUID> uuids, final UUID savedByUser) {
        caseAggregateState.addOffenceIdsForDefendant(defendantId, uuids);
        caseAggregateState.setDefendantId(defendantId);
        caseAggregateState.setDefendantFirstName(defendantFirstName);
        caseAggregateState.setDefendantLastName(defendantLastName);
        caseAggregateState.setAssigneeId(savedByUser);
        caseAggregateState.setCosts(BigDecimal.valueOf(40));
        caseAggregateState.setUrn(urn);
    }

    private void thenTheDecisionIsRejected(final Decision decision, final List<String> expectedRejectionReasons, final Stream<Object> eventStream) {
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList, hasItem(new DecisionRejected(decision, expectedRejectionReasons)));
    }

    private void thenTheDecisionIsNotRejected(final List<Object> events) {
        assertThat(events, everyItem(not(instanceOf(DecisionRejected.class))));
    }

    private void thenTheDecisionIsAccepted(final Decision decision, final List<Object> eventList) {
        assertThat(eventList, hasItem(new DecisionSaved(decisionId, sessionId, caseId, urn, savedAt, decision.getOffenceDecisions(),
                decision.getFinancialImposition(), defendantId, defendantFirstName + " " + defendantLastName, null)));
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseNoteAdded.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("decisionId", is(decisionId)),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("firstName", is("John"))),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("lastName", is("Smith"))),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("userId", is(legalAdviserId))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("addedAt", is(savedAt))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("id", is(any(UUID.class)))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("text", is(note))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("type", is(DECISION)))
        )));
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseCompleted.class),
                Matchers.<CaseCompleted>hasProperty("caseId", is(caseId)))));
        assertThat(eventList, hasItem(new CaseUnassigned(caseId)));
        assertThat(eventList.size(), is(4));
    }

    private void thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(final List<OffenceDecision> offenceDecisions, final List<Object> eventList, final LocalDate adjournedTo, final boolean shouldHaveConvictionDetails) {
        thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(decisionId, sessionId, offenceDecisions, eventList, adjournedTo, shouldHaveConvictionDetails);
    }

    private void thenTheDecisionIsAcceptedAlongWithCaseAdjournedRecordedEvent(final UUID decisionId, final UUID sessionId,
                                                                              final List<OffenceDecision> offenceDecisions,
                                                                              final List<Object> eventList, final LocalDate adjournedTo,
                                                                              final boolean shouldHaveConvictionDetails) {
        assertThat(eventList, hasItem(new DecisionSaved(decisionId, sessionId, caseId, urn, savedAt, offenceDecisions, defendantId, defendantFirstName + " " + defendantLastName)));
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(DecisionSaved.class),
                Matchers.<DecisionSaved>hasProperty("offenceDecisions"))));

        final DecisionSaved ds = (DecisionSaved) (eventList.get(0));
        if (shouldHaveConvictionDetails) {
            assertThat(ds.getOffenceDecisions(), hasItem(allOf(
                    Matchers.instanceOf(Adjourn.class),
                    Matchers.<OffenceDecision>hasProperty("convictingCourt", hasProperty("courtHouseCode", is("1008"))),
                    Matchers.<OffenceDecision>hasProperty("convictingCourt", hasProperty("ljaCode", is("1009")))
            )));
        }

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseNoteAdded.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("decisionId", is(decisionId)),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("firstName", is("John"))),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("lastName", is("Smith"))),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("userId", is(legalAdviserId))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("addedAt", is(savedAt))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("id", is(any(UUID.class)))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("text", is(note))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("type", is(DECISION)))
        )));
        assertThat(eventList, hasItem(new CaseAdjournedToLaterSjpHearingRecorded(adjournedTo, caseId, sessionId)));
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseNoteAdded.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("decisionId", is(decisionId)),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("firstName", is("John"))),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("lastName", is("Smith"))),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("userId", is(legalAdviserId))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("addedAt", is(savedAt))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("id", is(any(UUID.class)))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("text", is(adjournmentReason))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("type", is(ADJOURNMENT)))
        )));
        assertThat(eventList, hasItem(new CaseUnassigned(caseId)));
        assertThat(eventList.size(), is(5));
    }

    private void thenTheDecisionIsAcceptedAlongWithCaseReferForCourtHearingRecordedEvent(final List<OffenceDecision> offenceDecisions, final List<Object> eventList) {
        thenTheDecisionIsAcceptedAlongWithCaseReferForCourtHearingRecordedEvent(decisionId, sessionId, offenceDecisions, eventList);
    }

    private void thenTheDecisionIsAcceptedAlongWithCaseReferForCourtHearingRecordedEvent(final UUID decisionId, final UUID sessionId, final List<OffenceDecision> offenceDecisions, final List<Object> eventList) {
        assertThat(eventList, hasItem(new DecisionSaved(decisionId, sessionId, caseId, urn, savedAt, offenceDecisions, defendantId, defendantFirstName + " " + defendantLastName)));

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseNoteAdded.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("decisionId", is(decisionId)),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("firstName", is("John"))),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("lastName", is("Smith"))),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("userId", is(legalAdviserId))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("addedAt", is(savedAt))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("id", is(any(UUID.class)))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("text", is(note))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("type", is(DECISION)))
        )));
        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseNoteAdded.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("decisionId", is(decisionId)),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("firstName", is("John"))),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("lastName", is("Smith"))),
                Matchers.<CaseNoteAdded>hasProperty("author", hasProperty("userId", is(legalAdviserId))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("addedAt", is(savedAt))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("id", is(any(UUID.class)))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("text", is("note"))),
                Matchers.<CaseNoteAdded>hasProperty("note", hasProperty("type", is(LISTING)))
        )));

        assertThat(eventList, hasItem(new CaseUnassigned(caseId)));
        assertThat(eventList.size(), is(7));
    }

    private void thenTheOffenceDecisionsReflectTheConvictionDate(final List<OffenceDecision> offenceDecisions, final Class decisionType, final LocalDate convictionDate) {
        assertThat(offenceDecisions, hasItem(allOf(
                Matchers.instanceOf(decisionType),
                Matchers.<Adjourn>hasProperty("convictionDate", is(convictionDate))
        )));
    }
}


