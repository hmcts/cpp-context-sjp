package uk.gov.moj.sjp.it.test;

import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.disabilityNeedsOf;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.sjp.it.Constants.CASE_NOTE_ADDED_EVENT;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.createCase;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseCompleted;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseNotReadyInViewStore;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseQueryWithDisabilityNeeds;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseQueryWithReferForCourtHearingDecision;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseReferredForCourtHearing;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseUnassigned;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyDecisionSaved;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyHearingLanguagePreferenceUpdated;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyInterpreterUpdated;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyListingNotesAdded;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubBailStatuses;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubHearingTypesQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForAllProsecutors;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForVerdictTypes;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralDocumentMetadataQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralReason;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralReasonsQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContentAsJson;
import static org.hamcrest.Matchers.notNullValue;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.json.schemas.domains.sjp.events.CaseNoteAdded;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.CaseUnmarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.session.CaseUnassigned;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class CaseListedInCriminalCourtsIT extends BaseIntegrationTest {

    public static final String PUBLIC_PROGRESSION_PROSECUTION_CASES_REFERRED_TO_COURT = "public.progression.prosecution-cases-referred-to-court";
    public static final String PUBLIC_EVENT = "public.event";
    private final UUID caseId = randomUUID();
    private  EventListener eventListener;
    private UUID sessionId = randomUUID();
    private UUID offence1Id = randomUUID();
    private UUID offence2Id = randomUUID();
    private UUID offence3Id = randomUUID();
    private final User user = new User("Bob", "Bolt", USER_ID);
    private CreateCase.CreateCasePayloadBuilder aCase;
    private UUID defendantId;
    private LocalDate postingDate = LocalDate.now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);

    @Before
    public void setUp() throws Exception{
        eventListener = new EventListener();
        new SjpDatabaseCleaner().cleanViewStore();
        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubAssignmentReplicationCommands();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(user, "ALL");
        stubAllResultDefinitions();
        stubQueryForVerdictTypes();
        stubQueryForAllProsecutors();
        stubBailStatuses();
        stubResultIds();
        stubReferralDocumentMetadataQuery(randomUUID().toString(), "SJPN");

        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        aCase = createCase(caseId, offence1Id, offence2Id, offence3Id, postingDate);
        defendantId = aCase.getDefendantBuilder().getId();

    }

    @Test
    public void shouldUpdateCaseListedCriminalCourts() {

        final UUID referralReasonId = randomUUID();
        final UUID hearingTypeId = randomUUID();
        final String hearingCode = "PLE";
        final String referralReason = "Case unsuitable for SJP";
        final String hearingDescription = "PLE";

        stubReferralReasonsQuery(referralReasonId, hearingCode, referralReason);
        stubReferralReason(referralReasonId.toString(), "stub-data/referencedata.referral-reason.json");
        stubHearingTypesQuery(hearingTypeId.toString(), hearingCode, hearingDescription);

        final JsonObject session = startSessionAndRequestAssignment(sessionId, MAGISTRATE);

        final DisabilityNeeds disabilityNeeds = disabilityNeedsOf("Hearing aid");
        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("French", true), false, disabilityNeeds);
        final List<OffenceDecisionInformation> offenceDecisionInformationList = asList(new OffenceDecisionInformation(offence1Id, PROVED_SJP), new OffenceDecisionInformation(offence2Id, PROVED_SJP), new OffenceDecisionInformation(offence3Id, PROVED_SJP));
        final ReferForCourtHearing referForCourtHearing = new ReferForCourtHearing(
                null,
                offenceDecisionInformationList,
                referralReasonId, "listing notes", 30, defendantCourtOptions);

        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, asList(referForCourtHearing), null);

        final String hearingCourtName = "Carmarthen Magistrates' Court";
        final ZonedDateTime hearingTime = ZonedDateTime.now();
        final JsonObject payload1 = getFileContentAsJson("CaseListedInCriminalCourtsIT/case-listed-in-criminal-courts1.json",
                ImmutableMap.<String, Object>builder()
                        .put("prosecutionCaseId", caseId)
                        .put("offence1Id", offence1Id)
                        .put("offence2Id", offence2Id)
                        .put("offence3Id", offence3Id)
                        .put("name", hearingCourtName)
                        .put("welshName", hearingCourtName)
                        .put("sittingDay", ZonedDateTimes.toString(hearingTime))
                        .build());

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
        verifyCaseReferredForCourtHearing(decisionSaved, referForCourtHearing, caseReferredForCourtHearing, offenceDecisionInformationList, "Critical (Defendant has to attend)");
        verifyInterpreterUpdated(decisionSaved, referForCourtHearing, interpreterUpdatedForDefendant);
        verifyHearingLanguagePreferenceUpdated(decisionSaved, referForCourtHearing, hearingLanguagePreferenceUpdatedForDefendant);
        verifyCaseUnassigned(caseId, caseUnassigned);
        verifyCaseCompleted(caseId, caseCompleted);

        verifyCaseNotReadyInViewStore(caseId, USER_ID);

        verifyCaseQueryWithReferForCourtHearingDecision(decision, decisionSaved, session, referralReason, referForCourtHearing);
        verifyCaseQueryWithDisabilityNeeds(caseId, disabilityNeeds.getDisabilityNeeds());

        // first hearing
        eventListener = new EventListener();
        eventListener
                .subscribe("sjp.events.case-offence-listed-in-criminal-courts")
                .run(() -> raisePublicReferredToCourtEvent(payload1));

        Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent("sjp.events.case-offence-listed-in-criminal-courts");
        assertThat(jsonEnvelope.isPresent(), Matchers.is(true));

       final JsonObject payload2 = getFileContentAsJson("CaseListedInCriminalCourtsIT/case-listed-in-criminal-courts2.json",
                ImmutableMap.<String, Object>builder()
                        .put("prosecutionCaseId", caseId)
                        .put("offence3Id", offence3Id)
                        .put("name", hearingCourtName)
                        .put("welshName", hearingCourtName)
                        .put("sittingDay", ZonedDateTimes.toString(ZonedDateTime.now()))
                        .build());

        // second hearing
        eventListener = new EventListener();
        eventListener
                .subscribe("public.events.hearing.hearing-resulted")
                .run(() -> raisePublicReferredToCourtEvent(payload2));

        jsonEnvelope = eventListener.popEvent(PUBLIC_EVENTS_HEARING_HEARING_RESULTED);
        assertThat(jsonEnvelope.isPresent(), Matchers.is(true));
        assertThat(jsonEnvelope.get().payloadAsJsonObject().getJsonObject("hearing").getJsonArray("prosecutionCases").getJsonObject(0).getJsonArray("defendants").getJsonObject(0).getJsonArray("offences").getJsonObject(0).getJsonArray("judicialResults").getJsonObject(0).getJsonObject("nextHearing"), notNullValue());
    }

    private void raisePublicReferredToCourtEvent(final JsonObject payload) {
        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer(PUBLIC_EVENT);
            producerClient.sendMessage(PUBLIC_PROGRESSION_PROSECUTION_CASES_REFERRED_TO_COURT, payload);
        }
    }

    private static JsonObject startSessionAndRequestAssignment(final UUID sessionId, final SessionType sessionType) {
        final JsonEnvelope session = startSession(sessionId, USER_ID, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, sessionType).get();
        requestCaseAssignment(sessionId, USER_ID);
        return session.payloadAsJsonObject();
    }

}