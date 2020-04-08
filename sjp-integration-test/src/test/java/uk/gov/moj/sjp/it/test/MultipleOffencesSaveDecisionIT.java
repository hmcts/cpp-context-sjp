package uk.gov.moj.sjp.it.test;

import static java.lang.String.format;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.MONTH;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.sjp.it.Constants.CASE_ADJOURNED_TO_LATER_SJP_EVENT;
import static uk.gov.moj.sjp.it.Constants.CASE_NOTE_ADDED_EVENT;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_CASE_DECISION_SAVED_EVENT;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.createCase;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyAdjournmentNoteAdded;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseAdjourned;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseCompleted;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseNotReadyInViewStore;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseQueryWithAdjournDecision;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseQueryWithDismissDecision;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseQueryWithReferForCourtHearingDecision;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseQueryWithWithdrawnDecision;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseReferredForCourtHearing;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseUnassigned;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseUnmarkedReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyDecisionRejected;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyDecisionSaved;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyDecisionSavedPublicEventEmit;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyFinancialImposition;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyHearingLanguagePreferenceUpdated;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyInterpreterUpdated;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyListingNotesAdded;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyNoteAdded;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubHearingTypesQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralReasonsQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubWithdrawalReasonsQuery;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.LumpSum;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.event.CaseAdjournedToLaterSjpHearingRecorded;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseNoteAdded;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.decision.DecisionRejected;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.OffencesWithdrawalRequestHelper;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Enable this when merging to master")
public class MultipleOffencesSaveDecisionIT extends BaseIntegrationTest {

    private static final String ADJOURN_REASON = "Not enough documents present for decision, waiting for document";
    private static final String CASE_ALREADY_COMPLETED = "The case is already completed";
    private static final String CASE_NOT_ASSIGNED = "The case must be assigned to the caller";
    private static final String OFFENCE_ALREADY_HAS_DECISION = "Offence %s already has a final decision";
    private static final String REFERRAL_CANNOT_BE_SAVED_WITH_ADJOURN = "REFER_FOR_COURT_HEARING decision can not be saved with decision(s) ADJOURN";

    private UUID sessionId = randomUUID();
    private UUID caseId = randomUUID();
    private UUID offence1Id = randomUUID();
    private UUID offence2Id = randomUUID();
    private UUID offence3Id = randomUUID();
    private UUID withdrawalReasonId = randomUUID();
    private String withdrawalReason = "Insufficient evidence";
    private LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
    private final EventListener eventListener = new EventListener();
    private CreateCase.CreateCasePayloadBuilder aCase;
    private final User user = new User("John", "Smith", USER_ID);

    @Before
    public void setUp() throws Exception {
        caseId = randomUUID();
        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubAssignmentReplicationCommands();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(user, "ALL");

        new SjpDatabaseCleaner().cleanAll();

        aCase = createCase(caseId, offence1Id, offence2Id, offence3Id, postingDate);
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), "0000", "Any Court");
    }

    @Test
    public void shouldSaveWithdrawDecisionWithNote() throws Exception {
        stubWithdrawalReasonsQuery(withdrawalReasonId, withdrawalReason);

        try (final OffencesWithdrawalRequestHelper offencesWithdrawalRequestHelper = new OffencesWithdrawalRequestHelper(USER_ID)) {
            new EventListener()
                    .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                    .run(() -> offencesWithdrawalRequestHelper.requestWithdrawalOfOffences(caseId, offencesWithdrawalRequestHelper.preparePayloadWithDefaultsForCase(aCase)));
        }

        final JsonObject session = startSessionAndRequestAssignment(sessionId, DELEGATED_POWERS);

        final Withdraw withdrawOffence1 = new Withdraw(null, createOffenceDecisionInformation(offence1Id, NO_VERDICT), withdrawalReasonId);
        final Withdraw withdrawOffence2 = new Withdraw(null, createOffenceDecisionInformation(offence2Id, NO_VERDICT), withdrawalReasonId);
        final Withdraw withdrawOffence3 = new Withdraw(null, createOffenceDecisionInformation(offence3Id, NO_VERDICT), withdrawalReasonId);

        final List<Withdraw> offencesDecisions = asList(withdrawOffence1, withdrawOffence2, withdrawOffence3);

        final DecisionCommand decisionCommand = new DecisionCommand(sessionId, caseId, "Test note", user, offencesDecisions, null);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME, CASE_NOTE_ADDED_EVENT)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .subscribe(CASE_NOTE_ADDED_EVENT)
                .subscribe(PUBLIC_CASE_DECISION_SAVED_EVENT)
                .run(() -> saveDecision(decisionCommand));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseNoteAdded caseNoteAdded = eventListener.popEventPayload(CaseNoteAdded.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);
        final CaseCompleted caseCompleted = eventListener.popEventPayload(CaseCompleted.class);
        final JsonEnvelope decisionSavedEnvelope = eventListener.popEvent(PUBLIC_CASE_DECISION_SAVED_EVENT).get();

        verifyDecisionSaved(decisionCommand, decisionSaved);
        verifyNoteAdded(decisionCommand, decisionSaved, caseNoteAdded);
        verifyDecisionSavedPublicEventEmit(decisionSaved, decisionSavedEnvelope);
        verifyCaseCompleted(caseId, caseCompleted);
        verifyCaseUnassigned(caseId, caseUnassigned);

        verifyCaseNotReadyInViewStore(caseId, USER_ID);

        verifyCaseQueryWithWithdrawnDecision(decisionCommand, decisionSaved, session, offencesDecisions, withdrawalReason);
    }

    @Test
    public void shouldRejectWithdrawDecisionForAlreadyCompletedAndOffenceHasNoDecision() {
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final Withdraw withdrawOffence1 = new Withdraw(null, createOffenceDecisionInformation(offence1Id, NO_VERDICT), withdrawalReasonId);
        final Withdraw withdrawOffence2 = new Withdraw(null, createOffenceDecisionInformation(offence2Id, NO_VERDICT), withdrawalReasonId);
        final Withdraw withdrawOffence3 = new Withdraw(null, createOffenceDecisionInformation(offence3Id, NO_VERDICT), withdrawalReasonId);

        final List<OffenceDecision> offencesDecisions = asList(withdrawOffence1, withdrawOffence2, withdrawOffence3);

        final DecisionCommand firstDecision = new DecisionCommand(sessionId, caseId, "Test note", user, offencesDecisions, null);

        eventListener
                .subscribe(CaseCompleted.EVENT_NAME)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .run(() -> saveDecision(firstDecision));

        final List<OffenceDecision> newOffencesDecisions = asList(withdrawOffence1, withdrawOffence2);
        final DecisionCommand secondDecision = new DecisionCommand(sessionId, caseId, null, user, newOffencesDecisions, null);

        eventListener.reset()
                .subscribe(DecisionRejected.EVENT_NAME)
                .run(() -> saveDecision(secondDecision));

        final DecisionRejected decisionRejected = eventListener.popEventPayload(DecisionRejected.class);

        verifyDecisionRejected(secondDecision, decisionRejected,
                CASE_ALREADY_COMPLETED,
                CASE_NOT_ASSIGNED,
                format(OFFENCE_ALREADY_HAS_DECISION, offence1Id),
                format(OFFENCE_ALREADY_HAS_DECISION, offence2Id));
    }

    @Test
    public void shouldSaveAdjournDecisionForCase() {
        final JsonObject session = startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final LocalDate adjournTo = now().plusDays(10);

        final Adjourn adjournDecision = new Adjourn(randomUUID(),
                asList(
                        createOffenceDecisionInformation(offence1Id, NO_VERDICT),
                        createOffenceDecisionInformation(offence2Id, NO_VERDICT),
                        createOffenceDecisionInformation(offence3Id, NO_VERDICT)
                ),
                 ADJOURN_REASON, adjournTo);

        final List<Adjourn> offencesDecisions = singletonList(adjournDecision);

        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, null);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CASE_NOTE_ADDED_EVENT)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .subscribe(CASE_ADJOURNED_TO_LATER_SJP_EVENT)
                .subscribe(CaseUnmarkedReadyForDecision.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseNoteAdded caseNoteAdded = eventListener.popEventPayload(CaseNoteAdded.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);
        final CaseAdjournedToLaterSjpHearingRecorded caseAdjournedToLaterSjpHearingRecorded = eventListener.popEventPayload(CaseAdjournedToLaterSjpHearingRecorded.class);
        final CaseUnmarkedReadyForDecision caseUnmarkedReadyForDecision = eventListener.popEventPayload(CaseUnmarkedReadyForDecision.class);

        verifyDecisionSaved(decision, decisionSaved);
        verifyAdjournmentNoteAdded(decision, decisionSaved, adjournDecision, caseNoteAdded);
        verifyCaseAdjourned(decisionSaved, adjournDecision, caseAdjournedToLaterSjpHearingRecorded);
        verifyCaseUnassigned(caseId, caseUnassigned);
        verifyCaseUnmarkedReady(caseId, adjournDecision, caseUnmarkedReadyForDecision);

        verifyCaseNotReadyInViewStore(caseId, USER_ID);

        verifyCaseQueryWithAdjournDecision(decision, decisionSaved, session, adjournDecision);
    }

    @Test
    public void shouldSaveDismissDecision() {
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final Dismiss dismiss1 = new Dismiss(null, createOffenceDecisionInformation(offence1Id, FOUND_NOT_GUILTY));
        final Dismiss dismiss2 = new Dismiss(null, createOffenceDecisionInformation(offence2Id, FOUND_NOT_GUILTY));
        final Dismiss dismiss3 = new Dismiss(null, createOffenceDecisionInformation(offence3Id, FOUND_NOT_GUILTY));

        final List<Dismiss> offencesDecisions = asList(dismiss1, dismiss2, dismiss3);

        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, null);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .subscribe(CaseUnmarkedReadyForDecision.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);

        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseUnassigned(caseId, caseUnassigned);

        verifyCaseNotReadyInViewStore(caseId, USER_ID);
    }

    @Test
    public void shouldSaveDischargeDecision() {
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final Discharge discharge1 = Discharge.createDischarge(null, createOffenceDecisionInformation(offence1Id, FOUND_GUILTY), CONDITIONAL, new DischargePeriod(2, MONTH), new BigDecimal(230), null, false);
        final Discharge discharge2 = Discharge.createDischarge(null, createOffenceDecisionInformation(offence2Id, FOUND_GUILTY), ABSOLUTE, null, null, "No compensation reason", false);
        final Discharge discharge3 = Discharge.createDischarge(null, createOffenceDecisionInformation(offence3Id, PROVED_SJP), ABSOLUTE, null, BigDecimal.TEN, null, false);

        final List<Discharge> offencesDecisions = asList(discharge1, discharge2, discharge3);

        final FinancialImposition financialImposition = buildFinancialImposition();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, financialImposition);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .subscribe(CaseUnmarkedReadyForDecision.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);
        //after saving the decision the transfer of fine element is attached
        financialImposition.getPayment().setFineTransferredTo(new CourtDetails("1080","Bedfordshire Magistrates' Court"));
        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseUnassigned(caseId, caseUnassigned);

        verifyCaseNotReadyInViewStore(caseId, USER_ID);
    }

    private FinancialImposition buildFinancialImposition() {
        return new FinancialImposition(
                    new CostsAndSurcharge(new BigDecimal(40), null, new BigDecimal(100), null, "reason for reduced victim surcharge" ,false),
                    new Payment(new BigDecimal(370), PAY_TO_COURT, "Reason for not attached",null,
                            new PaymentTerms(false,
                                    new LumpSum(new BigDecimal(370), 5, LocalDate.of(2019, 7, 24)),
                                    new Installments(new BigDecimal(30), InstallmentPeriod.WEEKLY, LocalDate.of(2019, 7, 23))
                            ), null)
            );
    }

    @Test
    public void shouldSaveReferForCourtHearingDecision() {
        final UUID referralReasonId = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final String hearingCode = "PLE";
        final String referralReason = "Case unsuitable for SJP";
        final String hearingDescription = "PLE";

        stubReferralReasonsQuery(referralReasonId, hearingCode, referralReason);
        stubHearingTypesQuery(hearingTypeId.toString(), hearingCode, hearingDescription);
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");

        final JsonObject session = startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("French", true), false);
        final List<OffenceDecisionInformation> offenceDecisionInformationList = asList(new OffenceDecisionInformation(offence1Id, PROVED_SJP), new OffenceDecisionInformation(offence2Id, PROVED_SJP), new OffenceDecisionInformation(offence3Id, PROVED_SJP));
        final ReferForCourtHearing referForCourtHearing = new ReferForCourtHearing(
                null,
                offenceDecisionInformationList,
                referralReasonId, "listing notes", 30, defendantCourtOptions);

        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, asList(referForCourtHearing), null);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .subscribe(CaseUnmarkedReadyForDecision.EVENT_NAME)
                .subscribe(CaseReferredForCourtHearing.EVENT_NAME)
                .subscribe(InterpreterUpdatedForDefendant.EVENT_NAME)
                .subscribe(HearingLanguagePreferenceUpdatedForDefendant.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .subscribe(CASE_NOTE_ADDED_EVENT)
                .run(() -> DecisionHelper.saveDecision(decision));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseNoteAdded caseNoteAdded = eventListener.popEventPayload(CaseNoteAdded.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);
        final CaseReferredForCourtHearing caseReferredForCourtHearing = eventListener.popEventPayload(CaseReferredForCourtHearing.class);
        final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant = eventListener.popEventPayload(InterpreterUpdatedForDefendant.class);
        final HearingLanguagePreferenceUpdatedForDefendant hearingLanguagePreferenceUpdatedForDefendant = eventListener.popEventPayload(HearingLanguagePreferenceUpdatedForDefendant.class);
        final CaseCompleted caseCompleted = eventListener.popEventPayload(CaseCompleted.class);


        verifyDecisionSaved(decision, decisionSaved);
        verifyListingNotesAdded(decision, decisionSaved, referForCourtHearing, caseNoteAdded);
        verifyCaseReferredForCourtHearing(decisionSaved, referForCourtHearing, caseReferredForCourtHearing, offenceDecisionInformationList);
        verifyInterpreterUpdated(decisionSaved, referForCourtHearing, interpreterUpdatedForDefendant);
        verifyHearingLanguagePreferenceUpdated(decisionSaved, referForCourtHearing, hearingLanguagePreferenceUpdatedForDefendant);
        verifyCaseUnassigned(caseId, caseUnassigned);
        verifyCaseCompleted(caseId, caseCompleted);

        verifyCaseNotReadyInViewStore(caseId, USER_ID);

        verifyCaseQueryWithReferForCourtHearingDecision(decision, decisionSaved, session, referralReason, referForCourtHearing);
    }

    private static JsonObject startSessionAndRequestAssignment(final UUID sessionId, final SessionType sessionType) {
        final JsonEnvelope session = startSession(sessionId, USER_ID, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, sessionType).get();
        requestCaseAssignment(sessionId, USER_ID);
        return session.payloadAsJsonObject();
    }

    @Test
    public void shouldSaveAdjournDecisionWithWithdrawDecisionAndDismissDecision() throws Exception {
        final LocalDate adjournTo = now().plusDays(10);
        stubWithdrawalReasonsQuery(withdrawalReasonId, withdrawalReason);
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");

        final Adjourn adjournDecision = new Adjourn(null, asList(createOffenceDecisionInformation(offence1Id, NO_VERDICT)), ADJOURN_REASON, adjournTo);
        final Withdraw withdrawDecision = new Withdraw(null, createOffenceDecisionInformation(offence2Id, NO_VERDICT), withdrawalReasonId);
        final Dismiss dismissDecision = new Dismiss(null, createOffenceDecisionInformation(offence3Id, FOUND_NOT_GUILTY));

        final JsonObject session = startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final List<OffenceDecision> offencesDecisions = asList(adjournDecision, withdrawDecision, dismissDecision);
        final DecisionCommand decisionCommand = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, null);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CASE_NOTE_ADDED_EVENT)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .subscribe(CASE_ADJOURNED_TO_LATER_SJP_EVENT)
                .subscribe(CaseUnmarkedReadyForDecision.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decisionCommand));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseNoteAdded caseNoteAdded = eventListener.popEventPayload(CaseNoteAdded.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);
        final CaseAdjournedToLaterSjpHearingRecorded caseAdjournedToLaterSjpHearingRecorded = eventListener.popEventPayload(CaseAdjournedToLaterSjpHearingRecorded.class);
        final CaseUnmarkedReadyForDecision caseUnmarkedReadyForDecision = eventListener.popEventPayload(CaseUnmarkedReadyForDecision.class);

        verifyDecisionSaved(decisionCommand, decisionSaved);
        verifyAdjournmentNoteAdded(decisionCommand, decisionSaved, adjournDecision, caseNoteAdded);
        verifyCaseAdjourned(decisionSaved, adjournDecision, caseAdjournedToLaterSjpHearingRecorded);
        verifyCaseUnassigned(caseId, caseUnassigned);
        verifyCaseUnmarkedReady(caseId, adjournDecision, caseUnmarkedReadyForDecision);
        verifyCaseNotReadyInViewStore(caseId, USER_ID);

        verifyCaseQueryWithAdjournDecision(decisionCommand, decisionSaved, session, adjournDecision);
        verifyCaseQueryWithWithdrawnDecision(decisionCommand, decisionSaved, session, asList(withdrawDecision), withdrawalReason);
        verifyCaseQueryWithDismissDecision(decisionCommand, decisionSaved, session, dismissDecision);
    }

    @Test
    public void shouldSaveReferForCourtHearingDecisionWithWithdrawDecisionAndDismissDecision() throws Exception {
        final UUID referralReasonId = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final String hearingCode = "PLE";
        final String referralReason = "Case unsuitable for SJP";
        final String hearingDescription = "PLE";

        stubReferralReasonsQuery(referralReasonId, hearingCode, referralReason);
        stubHearingTypesQuery(hearingTypeId.toString(), hearingCode, hearingDescription);
        stubWithdrawalReasonsQuery(withdrawalReasonId, withdrawalReason);

        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("French", true), false);
        final List<OffenceDecisionInformation> offenceDecisionInformationList = asList(new OffenceDecisionInformation(offence1Id, PROVED_SJP));
        final ReferForCourtHearing referForCourtHearingDecision = new ReferForCourtHearing(null, asList(createOffenceDecisionInformation(offence1Id, PROVED_SJP)), referralReasonId, "listing notes", 30, defendantCourtOptions);
        final Withdraw withdrawDecision = new Withdraw(null, createOffenceDecisionInformation(offence2Id, NO_VERDICT), withdrawalReasonId);
        final Dismiss dismissDecision = new Dismiss(null, createOffenceDecisionInformation(offence3Id, FOUND_NOT_GUILTY));

        final JsonObject session = startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final List<OffenceDecision> offencesDecisions = asList(referForCourtHearingDecision, withdrawDecision, dismissDecision);
        final DecisionCommand decisionCommand = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, null);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CASE_NOTE_ADDED_EVENT)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .subscribe(CASE_ADJOURNED_TO_LATER_SJP_EVENT)
                .subscribe(CaseUnmarkedReadyForDecision.EVENT_NAME)
                .subscribe(CaseReferredForCourtHearing.EVENT_NAME)
                .subscribe(InterpreterUpdatedForDefendant.EVENT_NAME)
                .subscribe(HearingLanguagePreferenceUpdatedForDefendant.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decisionCommand));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseNoteAdded caseNoteAdded = eventListener.popEventPayload(CaseNoteAdded.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);
        final CaseReferredForCourtHearing caseReferredForCourtHearing = eventListener.popEventPayload(CaseReferredForCourtHearing.class);
        final InterpreterUpdatedForDefendant interpreterUpdatedForDefendant = eventListener.popEventPayload(InterpreterUpdatedForDefendant.class);
        final HearingLanguagePreferenceUpdatedForDefendant hearingLanguagePreferenceUpdatedForDefendant = eventListener.popEventPayload(HearingLanguagePreferenceUpdatedForDefendant.class);
        final CaseCompleted caseCompleted = eventListener.popEventPayload(CaseCompleted.class);

        verifyDecisionSaved(decisionCommand, decisionSaved);
        verifyCaseUnassigned(caseId, caseUnassigned);
        verifyCaseNotReadyInViewStore(caseId, USER_ID);
        verifyListingNotesAdded(decisionCommand, decisionSaved, referForCourtHearingDecision, caseNoteAdded);
        verifyCaseReferredForCourtHearing(decisionSaved, referForCourtHearingDecision, caseReferredForCourtHearing, offenceDecisionInformationList);
        verifyInterpreterUpdated(decisionSaved, referForCourtHearingDecision, interpreterUpdatedForDefendant);
        verifyHearingLanguagePreferenceUpdated(decisionSaved, referForCourtHearingDecision, hearingLanguagePreferenceUpdatedForDefendant);
        verifyCaseCompleted(caseId, caseCompleted);

        verifyCaseNotReadyInViewStore(caseId, USER_ID);

        verifyCaseQueryWithReferForCourtHearingDecision(decisionCommand, decisionSaved, session, referralReason, referForCourtHearingDecision);
        verifyCaseQueryWithWithdrawnDecision(decisionCommand, decisionSaved, session, asList(withdrawDecision), withdrawalReason);
        verifyCaseQueryWithDismissDecision(decisionCommand, decisionSaved, session, dismissDecision);
    }

    @Test
    public void shouldRejectReferForCourtHearingDecisionSavedWithAdjournDecision() {
        final LocalDate adjournTo = now().plusDays(10);
        final UUID referralReasonId = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final String hearingCode = "PLE";
        final String referralReason = "Case unsuitable for SJP";
        final String hearingDescription = "PLE";

        stubReferralReasonsQuery(referralReasonId, hearingCode, referralReason);
        stubHearingTypesQuery(hearingTypeId.toString(), hearingCode, hearingDescription);
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");

        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("French", true), false);
        final ReferForCourtHearing referForCourtHearingDecisions = new ReferForCourtHearing(null, asList(createOffenceDecisionInformation(offence1Id, PROVED_SJP)), referralReasonId, "listing notes", 30, defendantCourtOptions);
        final Adjourn adjournDecisions = new Adjourn(null, asList(createOffenceDecisionInformation(offence2Id, FOUND_NOT_GUILTY), createOffenceDecisionInformation(offence3Id, FOUND_NOT_GUILTY)), ADJOURN_REASON, adjournTo);

        final List<OffenceDecision> offencesDecisions = asList(referForCourtHearingDecisions, adjournDecisions);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, null);

        eventListener.reset()
                .subscribe(DecisionRejected.EVENT_NAME)
                .run(() -> saveDecision(decision));

        final DecisionRejected decisionRejected = eventListener.popEventPayload(DecisionRejected.class);

        verifyDecisionRejected(decision, decisionRejected,
                CASE_NOT_ASSIGNED,
                REFERRAL_CANNOT_BE_SAVED_WITH_ADJOURN);
    }

    @Test
    public void shouldSaveFinancialPenaltyDecision() {
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final FinancialPenalty fp1 = FinancialPenalty.createFinancialPenalty(null, createOffenceDecisionInformation(offence1Id, FOUND_GUILTY), BigDecimal.ZERO, BigDecimal.TEN, null, true);
        final FinancialPenalty fp2 = FinancialPenalty.createFinancialPenalty(null, createOffenceDecisionInformation(offence2Id, FOUND_GUILTY), BigDecimal.TEN, BigDecimal.ZERO, "some reason", true);
        final FinancialPenalty fp3 = FinancialPenalty.createFinancialPenalty(null, createOffenceDecisionInformation(offence3Id, PROVED_SJP), BigDecimal.TEN, BigDecimal.TEN, null, true);

        final List<FinancialPenalty> offencesDecisions = asList(fp1, fp2, fp3);

        final FinancialImposition financialImposition = buildFinancialImposition();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, financialImposition);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);

        //after saving the decision the transfer of fine element is attached
        financialImposition.getPayment().setFineTransferredTo(new CourtDetails("1080","Bedfordshire Magistrates' Court"));

        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseUnassigned(caseId, caseUnassigned);
        verifyCaseNotReadyInViewStore(caseId, USER_ID);
    }

    @Test
    public void shouldSaveFinancialImposition() {
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final FinancialPenalty fp1 = FinancialPenalty.createFinancialPenalty(null, createOffenceDecisionInformation(offence1Id, FOUND_GUILTY), BigDecimal.ZERO, BigDecimal.TEN, null, true);
        final FinancialPenalty fp2 = FinancialPenalty.createFinancialPenalty(null, createOffenceDecisionInformation(offence2Id, FOUND_GUILTY), BigDecimal.TEN, BigDecimal.ZERO, "some reason", true);
        final FinancialPenalty fp3 = FinancialPenalty.createFinancialPenalty(null, createOffenceDecisionInformation(offence3Id, PROVED_SJP), BigDecimal.TEN, BigDecimal.TEN, null, true);

        final List<FinancialPenalty> offencesDecisions = asList(fp1, fp2, fp3);

        FinancialImposition financialImposition = buildFinancialImposition();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, financialImposition);


        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);

        //after saving the decision the transfer of fine element is attached
        financialImposition.getPayment().setFineTransferredTo(new CourtDetails("1080","Bedfordshire Magistrates' Court"));

        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseUnassigned(caseId, caseUnassigned);
        verifyCaseNotReadyInViewStore(caseId, USER_ID);

        verifyFinancialImposition(decisionSaved,financialImposition);
    }

    @Test
    public void shouldSaveOffenceDecisionWithBackDutyAndExcisePenalty() {
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final OffenceDecision financialPenalty = new FinancialPenalty(null, createOffenceDecisionInformation(offence1Id, PROVED_SJP), BigDecimal.ONE, null, null,
                true, BigDecimal.ONE, BigDecimal.ONE);
        final Discharge discharge = new Discharge(null, createOffenceDecisionInformation(offence2Id, PROVED_SJP), ABSOLUTE, null, BigDecimal.TEN,
                null, false, new BigDecimal(25));
        final Dismiss dismissDecision = new Dismiss(null, createOffenceDecisionInformation(offence3Id, FOUND_NOT_GUILTY));

        final List<OffenceDecision> offencesDecisions = asList(financialPenalty, discharge, dismissDecision);

        final FinancialImposition financialImposition = buildFinancialImposition();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, financialImposition);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);

        //after saving the decision the transfer of fine element is attached
        financialImposition.getPayment().setFineTransferredTo(new CourtDetails("1080","Bedfordshire Magistrates' Court"));

        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseUnassigned(caseId, caseUnassigned);
        verifyCaseNotReadyInViewStore(caseId, USER_ID);
    }

    @Test
    public void shouldSaveOffenceDecisionWithBackDutyAndExcisePenaltyWithValidFine() {
        stubEnforcementAreaByPostcode(aCase.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final OffenceDecision fp = new FinancialPenalty(null, createOffenceDecisionInformation(offence1Id, FOUND_GUILTY), BigDecimal.valueOf(1001), BigDecimal.ONE, null, true, new BigDecimal(10), new BigDecimal(20));
        final Discharge discharge = new Discharge(null, createOffenceDecisionInformation(offence2Id, PROVED_SJP), ABSOLUTE, null, BigDecimal.TEN,
                null, false, new BigDecimal(25));
        final Dismiss dismissDecision = new Dismiss(null, createOffenceDecisionInformation(offence3Id, FOUND_NOT_GUILTY));

        final List<OffenceDecision> offencesDecisions = asList(fp, discharge, dismissDecision);

        final FinancialImposition financialImposition = buildFinancialImposition();
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, financialImposition);

        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseUnassigned.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        final CaseUnassigned caseUnassigned = eventListener.popEventPayload(CaseUnassigned.class);

        //after saving the decision the transfer of fine element is attached
        financialImposition.getPayment().setFineTransferredTo(new CourtDetails("1080","Bedfordshire Magistrates' Court"));

        verifyDecisionSaved(decision, decisionSaved);
        verifyCaseUnassigned(caseId, caseUnassigned);
        verifyCaseNotReadyInViewStore(caseId, USER_ID);
    }

}
