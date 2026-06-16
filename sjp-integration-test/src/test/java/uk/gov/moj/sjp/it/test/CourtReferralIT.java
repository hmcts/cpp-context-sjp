package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.core.courts.CourtCentre.courtCentre;
import static uk.gov.justice.core.courts.HearingType.hearingType;
import static uk.gov.justice.core.courts.NextHearing.nextHearing;
import static uk.gov.justice.json.schemas.domains.sjp.Language.W;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.NO_DISABILITY_NEEDS;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.disabilityNeedsOf;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.EmployerHelper.pollForEmployerForDefendant;
import static uk.gov.moj.sjp.it.helper.PleadOnlineHelper.verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollForCase;
import static uk.gov.moj.sjp.it.stub.IdMapperStub.stubForIdMapperSuccess;
import static uk.gov.moj.sjp.it.stub.MaterialStub.stubMaterialMetadata;
import static uk.gov.moj.sjp.it.stub.ProgressionServiceStub.stubReferCaseToCourtCommand;
import static uk.gov.moj.sjp.it.stub.ProgressionServiceStub.verifyReferToCourtCommandSent;
import static uk.gov.moj.sjp.it.stub.ProgressionServiceStub.verifyReferToCourtCommandSentStrictMode;
import static uk.gov.moj.sjp.it.stub.ProsecutionCaseFileServiceStub.stubCaseDetails;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryByPostcodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEthnicities;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubHearingTypesQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubOffenceFineLevelsQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffencesByCode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralDocumentMetadataQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralReason;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralReasonsQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultDefinitionByResultCode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.test.CaseListedInCriminalCourtsIT.PUBLIC_PROGRESSION_PROSECUTION_CASES_REFERRED_TO_COURT;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.DateUtils.CPP_ZONED_DATE_TIME_FORMATTER;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanAll;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;

import uk.gov.justice.core.courts.NextHearing;
import uk.gov.justice.core.courts.RotaSlot;
import uk.gov.justice.json.schemas.domains.sjp.Language;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.common.converter.ZonedDateTimes;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.integrationtest.utils.jms.JmsResourceManagementExtension;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.CreateCase.DefendantBuilder;
import uk.gov.moj.sjp.it.command.CreateCase.OffenceBuilder;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.EmployerHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.PleadOnlineHelper;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.producer.ReferToCourtHearingProducer;
import uk.gov.moj.sjp.it.util.FileUtil;
import uk.gov.moj.sjp.it.util.builders.DismissBuilder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.apache.commons.lang3.RandomStringUtils;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JmsResourceManagementExtension.class)
public class CourtReferralIT extends BaseIntegrationTest {

    private static final UUID REFERRAL_REASON_ID = randomUUID();
    private static final String HEARING_CODE = "PLE";
    private static final String REFERRAL_REASON = "referral reason";
    private static final ZonedDateTime RESULTED_ON = now(UTC);

    private static final UUID HEARING_TYPE_ID = fromString("06b0c2bf-3f98-46ed-ab7e-56efaf9ecced");
    private static final String HEARING_DESCRIPTION = "Plea & Trial Preparation";

    private static final UUID PROSECUTOR_ID = randomUUID();
    private static final String LIBRA_OFFENCE_CODE1 = "PS00001";
    private static final String LIBRA_OFFENCE_CODE2 = "CA03010";

    private static final UUID DOCUMENT_TYPE_ID = randomUUID();
    private static final String DOCUMENT_TYPE = "SJPN";
    private static final UUID MATERIAL_ID = randomUUID();
    private static final UUID DOCUMENT_ID = randomUUID();
    private static final String FILE_NAME = "Bank Statement";
    private static final String MIME_TYPE = "pdf";
    private static final ZonedDateTime ADDED_AT = new UtcClock().now();
    private static final String REFERENCE_DATA_DOCUMENT_TYPE = "Case Summary";

    private static final String NATIONAL_INSURANCE_NUMBER = "BB333333B";
    private JsonObject employerDetails;

    private static final User user = new User("John", "Smith", USER_ID);
    private static final String DISABILITY_NEEDS = "Hearing aid";

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private UUID defendantId;
    private ProsecutingAuthority prosecutingAuthority;
    private String caseUrn;

    private UUID sessionId;
    private UUID caseId;
    private UUID offenceId1;
    private UUID offenceId2;
    private UUID offenceId3;
    private static final String NATIONAL_COURT_CODE = "1080";
    private NextHearing nextHearing;
    private UUID roomId;
    private ZonedDateTime listedStartDateTime;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    private EventListener eventListener;

    private final String employeeReference = RandomStringUtils.random(8);

    @BeforeAll
    public static void setup() throws SQLException {
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubQueryOffencesByCode(LIBRA_OFFENCE_CODE2);
        stubReferralReasonsQuery(REFERRAL_REASON_ID, HEARING_CODE, REFERRAL_REASON);
        stubReferralReason(REFERRAL_REASON_ID.toString(), "stub-data/referencedata.referral-reason.json");
        stubHearingTypesQuery(HEARING_TYPE_ID.toString(), HEARING_CODE, HEARING_DESCRIPTION);
        stubCountryNationalities("stub-data/referencedata.query.country-nationality.json");
        stubEthnicities("stub-data/referencedata.query.ethnicities.json");
        stubReferralDocumentMetadataQuery(DOCUMENT_TYPE_ID.toString(), REFERENCE_DATA_DOCUMENT_TYPE);
        stubMaterialMetadata(MATERIAL_ID, FILE_NAME, MIME_TYPE, ADDED_AT);
        stubReferCaseToCourtCommand();
        stubForIdMapperSuccess(Response.Status.OK);
        stubForUserDetails(USER_ID, "ALL");
        stubForUserDetails(fromString("7242d476-9ca3-454a-93ee-78bf148602bf"), "ALL");
        stubCountryByPostcodeQuery("W1T 1JY", "England");
        stubResultDefinitionByResultCode("DC");
        cleanAll();
        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
    }

    @BeforeEach
    public void setUp() throws Exception {
        employerDetails = createEmployerDetails();

        eventListener = new EventListener();
        sessionId = randomUUID();
        caseId = randomUUID();
        offenceId1 = randomUUID();
        offenceId2 = randomUUID();
        offenceId3 = randomUUID();
        defendantId = randomUUID();
        roomId = randomUUID();
        listedStartDateTime = new UtcClock().now().plusHours(2);

        nextHearing = nextHearing()
                .withType(hearingType().withId(fromString("bf8155e1-90b9-4080-b133-bfbad895d6e4")).withDescription("Trial").build())
                .withEstimatedMinutes(20)
                .withListedStartDateTime(listedStartDateTime)
                .withCourtCentre(courtCentre()
                        .withId(fromString("4b6185e1-92f2-3634-b749-87d2fcadf1c8"))
                        .withName("Basingstoke Magistrates' Court").withRoomId(roomId).build())
                .build();

    }

    @Test
    public void shouldRecordCaseReferralRejection() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-welsh.json");
        createCaseWithSingleOffence();
        addCaseDocumentAndUpdateEmployer();

        final String referralRejectionReason = "Test referral rejection reason";

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);

        final ReferToCourtHearingProducer referToCourtHearingProducer = new ReferToCourtHearingProducer(caseId, REFERRAL_REASON_ID, HEARING_TYPE_ID, referralRejectionReason);

        final ReferForCourtHearing referForCourtHearingDecision = buildReferForCourtHearingDecision(W, nextHearing, offenceId1);

        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, List.of(referForCourtHearingDecision), null);
        saveDecision(decision);
        pollForCase(caseId, new Matcher[]{withJsonPath("$.managedByATCM", is(false))});

        referToCourtHearingProducer.rejectCaseReferral();
        pollForCase(caseId, new Matcher[]{withJsonPath("$.managedByATCM", is(true))});
    }

    @Test
    public void shouldSendReferToCourtHearingCommandToProgressionContext_NextHearingWithHmiSlots_NoPlea() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-welsh.json");
        createCaseWithSingleOffence();
        addCaseDocumentAndUpdateEmployer();

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);
        nextHearing = getNextHearingWithSlots();

        referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected("payload/referral/progression.refer-for-court-hearing_next-hearing_with_slots_no-plea.json", nextHearing, W);
    }

    @Test
    public void shouldSendReferToCourtHearingCommandToProgressionContext_NoPlea_WithProvedSJPVerdict() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-welsh.json");
        createCaseWithSingleOffence();
        addCaseDocumentAndUpdateEmployer();

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);
        referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected_WithVerdict();
    }

    @Test
    public void shouldSendReferToCourtHearingCommandToProgressionContext_PoliceCase() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-police.json");
        createCaseWithSingleOffence(true);
        addCaseDocumentAndUpdateEmployer();

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);
        referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected("payload/referral/progression.refer-for-court-hearing_next-hearing_police.json", nextHearing, W);
    }

    @Test
    public void shouldSendReferToCourtHearingCommandToProgressionContext_WithPlea() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details.json");
        createCaseWithSingleOffence();
        addCaseDocumentAndUpdateEmployer();

        try (PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(caseId)) {
            pleadOnlineHelper.pleadOnline(getPayload("raml/json/sjp.command.plead-online__guilty_request_hearing.json")
                    .replace("ecf30a03-8a17-4fc5-81d2-b72ac0a13d17", offenceId1.toString())
                    .replace("AB123456A", NATIONAL_INSURANCE_NUMBER));
        }

        verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(caseId, true);

        startSessionAndRequestAssignment(sessionId, DELEGATED_POWERS, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);
        referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected("payload/referral/progression.refer-for-court-hearing_next-hearing_plea-present.json", nextHearing, W);
    }

    @Test
    public void shouldSendReferToCourtHearingCommandToProgressionContext_WithReportingRestrictions() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-welsh.json");

        createCaseWithSingleOffence(false, true);

        final ReferForCourtHearing referForCourtHearingDecision = buildReferForCourtHearingDecision(
                PressRestriction.requested("a name"),
                offenceId1);

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);

        final List<OffenceDecision> offencesDecisions = List.of(referForCourtHearingDecision);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, null);

        saveDecision(decision);

        final JsonObject expectedCommandPayload = prepareExpectedCommandPayload("payload/referral/progression.refer-for-court-hearing_reporting-restrictions.json", W);
        verifyReferToCourtCommandSentStrictMode(expectedCommandPayload, List.of("courtReferral.prosecutionCases[0].defendants[0].offences[0].reportingRestrictions[0].id"));
    }

    @Test
    public void shouldSendReferToCourtHearingCommandToProgressionContext_withDisabiltyStatus() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details.json");
        createCaseWithSingleOffence();
        addCaseDocumentAndUpdateEmployer();

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);
        referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpectedWithDisaNeeded();
    }

    @Test
    public void shouldSendReferToCourtHearingCommandToProgressionContext_withDisabiltyStatusWHENNoSpecialRequirment() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-without-specialRequirement.json");
        createCaseWithSingleOffence();
        addCaseDocumentAndUpdateEmployer();

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);
        referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpectedWithDisaNeeded();
    }

    @Test
    public void shouldReferMultipleOffencesForCourtHearing() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-welsh.json");
        stubOffenceFineLevelsQuery(3, BigDecimal.valueOf(1000));
        createCaseWithMultipleOffences();
        addCaseDocumentAndUpdateEmployer();

        final ReferForCourtHearing referForCourtHearingDecision = buildReferForCourtHearingDecision(W, nextHearing, offenceId1, offenceId2);
        final Dismiss dismissDecision = DismissBuilder.withDefaults(offenceId3).build();

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE);

        final List<OffenceDecision> offencesDecisions = asList(referForCourtHearingDecision, dismissDecision);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, null);

        saveDecision(decision);

        final JsonObject expectedCommandPayload = prepareExpectedCommandPayload("payload/referral/progression.refer-for-court-hearing_next-hearing_multiple-offences.json", W);
        verifyReferToCourtCommandSent(expectedCommandPayload);
    }

    @Test
    public void shouldReferOrganisationCaseSuccessfully() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-welsh.json");
        createLegalEntityCaseWithSingleOffence();

        final ReferForCourtHearing referForCourtHearingDecision = buildReferForCourtHearingDecision(
                PressRestriction.requested("a name"),
                offenceId1);

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);

        final List<OffenceDecision> offencesDecisions = List.of(referForCourtHearingDecision);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, null);
        saveDecision(decision);
        pollForCase(caseId, new Matcher[]{withJsonPath("$.managedByATCM", is(false))});

        final String hearingCourtName = "Leamington Spa Magistrates' Court";
        final JsonObject payload1 = getFileContentAsJson("CourtReferralIT/case-listed-in-criminal-courts1.json",
                ImmutableMap.<String, Object>builder()
                        .put("prosecutionCaseId", caseId)
                        .put("offence1Id", offenceId1)
                        .put("name", hearingCourtName)
                        .put("welshName", hearingCourtName)
                        .put("sittingDay", ZonedDateTimes.toString(ZonedDateTime.now()))
                        .build());

        // first hearing
        eventListener = new EventListener();
        eventListener
                .subscribe("sjp.events.case-offence-listed-in-criminal-courts")
                .run(() -> raisePublicReferredToCourtEvent(payload1));

        Optional<JsonEnvelope> jsonEnvelope = eventListener.popEvent("sjp.events.case-offence-listed-in-criminal-courts");
        assertThat(jsonEnvelope.isPresent(), Matchers.is(true));
    }


    private void raisePublicReferredToCourtEvent(final JsonObject payload) {
        final JmsMessageProducerClient publicJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                .getMessageProducerClient();
        publicJmsMessageProducerClient.sendMessage(PUBLIC_PROGRESSION_PROSECUTION_CASES_REFERRED_TO_COURT, payload);
    }

    private void referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected(final String expectedCommandPayloadFile, final NextHearing nextHearing, final Language language) {
        final ReferForCourtHearing referForCourtHearingDecision = buildReferForCourtHearingDecision(language, nextHearing, offenceId1);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, List.of(referForCourtHearingDecision), null);

        saveDecision(decision);

        final JsonObject expectedCommandPayload = prepareExpectedCommandPayload(expectedCommandPayloadFile, language);

        verifyReferToCourtCommandSent(expectedCommandPayload);
    }

    private void referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected_WithVerdict() {
        final ReferForCourtHearing referForCourtHearingDecision = buildReferForCourtHearingDecision_WithVerdictType(offenceId1);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, List.of(referForCourtHearingDecision), null);

        saveDecision(decision);

        final JsonObject expectedCommandPayload = prepareExpectedCommandPayload("payload/referral/progression.refer-for-court-hearing_no-plea_with_verdict.json", Language.W);

        verifyReferToCourtCommandSent(expectedCommandPayload);
    }

    private void referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpectedWithDisaNeeded() {
        final ReferForCourtHearing referForCourtHearingDecision = buildReferForCourtHearingWithDisaNeededDecision(offenceId1);
        List<ReferForCourtHearing> referForCourtHearings = new java.util.ArrayList<>();
        referForCourtHearings.add(referForCourtHearingDecision);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, referForCourtHearings, null);

        saveDecision(decision);

        final JsonObject expectedCommandPayload = prepareExpectedCommandPayload("payload/referral/progression.refer-for-court-hearing-with-disability-needed.json", Language.W);

        verifyReferToCourtCommandSent(expectedCommandPayload);
    }

    private JsonObject prepareExpectedCommandPayload(String payloadFileLocation, Language language) {
        return FileUtil.getFileContentAsJson(payloadFileLocation, ImmutableMap.<String, Object>builder()
                .put("OFFENCE_ID1", offenceId1.toString())
                .put("OFFENCE_ID2", offenceId2.toString())
                .put("OFFENCE_ID3", offenceId3.toString())
                .put("CASE_ID", caseId.toString())
                .put("DEFENDANT_ID", defendantId)
                .put("PROSECUTING_AUTHORITY_REFERENCE", caseUrn)
                .put("PROSECUTING_AUTHORITY_ID", PROSECUTOR_ID.toString())
                .put("HEARING_TYPE_ID", HEARING_TYPE_ID.toString())
                .put("REFERRAL_REASON_ID", REFERRAL_REASON_ID.toString())
                .put("LISTING_NOTES", "listing notes")
                .put("CONVICTION_DATE", RESULTED_ON.toLocalDate().toString())
                .put("MAGISTRATE_ID", sessionId.toString())
                .put("PLEA_DATE", LocalDate.now().toString())
                .put("REFERRAL_DATE", LocalDate.now().toString())
                .put("DOCUMENT_ID", DOCUMENT_ID.toString())
                .put("DOCUMENT_TYPE_ID", DOCUMENT_TYPE_ID.toString())
                .put("MATERIAL_ID", MATERIAL_ID.toString())
                .put("UPLOAD_DATE_TIME", ADDED_AT.format(CPP_ZONED_DATE_TIME_FORMATTER))
                .put("NINO", NATIONAL_INSURANCE_NUMBER)
                .put("HEARING_LANGUAGE", (language.equals(W) ? "WELSH" : "ENGLISH"))
                .put("DRIVER_NUMBER", createCasePayloadBuilder.getDefendantBuilder().getDriverNumber())
                .put("ROOM_ID", nextHearing.getCourtCentre().getRoomId().toString())
                .put("LISTED_START_DATE", nextHearing.getListedStartDateTime().format(FORMATTER))
                .build());
    }

    private JsonObject createEmployerDetails() {
        final JsonObject address = createObjectBuilder()
                .add("address1", "Foo")
                .add("address2", "Flat 8")
                .add("address3", "Lant House")
                .add("address4", "London")
                .add("address5", "Greater London")
                .add("postcode", "SE1 1PJ").build();

        return createObjectBuilder()
                .add("name", "Test Org")
                .add("employeeReference", employeeReference)
                .add("phone", "02020202020")
                .add("address", address).build();
    }

    private void startSessionAndRequestAssignment(final UUID sessionId, final SessionType sessionType, final String courtHouseCode) {
        startSessionAndConfirm(sessionId, USER_ID, courtHouseCode, sessionType);
        requestCaseAssignmentAndConfirm(sessionId, USER_ID, caseId);
    }

    private void createCaseWithSingleOffence() {
        createCaseWithSingleOffence(false);
    }

    private void createCaseWithSingleOffence(boolean policeFlag) {
        createCaseWithSingleOffence(policeFlag, false);
    }

    private void createCaseWithSingleOffence(boolean policeFlag, boolean pressRestrictable) {
        prosecutingAuthority = ProsecutingAuthority.TFL;
        caseUrn = generate(prosecutingAuthority);
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), PROSECUTOR_ID, policeFlag);
        stubForUserDetails(user, prosecutingAuthority.name());

        createCasePayloadBuilder = withDefaults()
                .withProsecutingAuthority(prosecutingAuthority)
                .withOffenceBuilder(OffenceBuilder.withDefaults()
                        .withId(offenceId1)
                        .withLibraOffenceCode(LIBRA_OFFENCE_CODE1)
                        .withPressRestrictable(pressRestrictable)
                )
                .withDefendantBuilder(DefendantBuilder.withDefaults()
                        .withId(defendantId)
                        .withNationalInsuranceNumber(NATIONAL_INSURANCE_NUMBER)
                        .withHearingLanguage(Language.W))
                .withId(caseId)
                .withUrn(caseUrn);

        stubRegionByPostcode(NATIONAL_COURT_CODE, "DEFENDANT_REGION");
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);
    }


    private void createLegalEntityCaseWithSingleOffence() {
        prosecutingAuthority = ProsecutingAuthority.TFL;
        caseUrn = generate(prosecutingAuthority);
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), PROSECUTOR_ID, false);
        stubForUserDetails(user, prosecutingAuthority.name());

        createCasePayloadBuilder = withDefaults()
                .withProsecutingAuthority(prosecutingAuthority)
                .withOffenceBuilder(OffenceBuilder.withDefaults()
                        .withId(offenceId1)
                        .withLibraOffenceCode(LIBRA_OFFENCE_CODE1)
                        .withPressRestrictable(false)
                )
                .withDefendantBuilder(DefendantBuilder
                        .defaultLegalEntityDefendant()
                        .withHearingLanguage(W)
                )
                .withId(caseId)
                .withUrn(caseUrn);

        stubRegionByPostcode(NATIONAL_COURT_CODE, "DEFENDANT_REGION");
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);
    }

    private void createCaseWithMultipleOffences() {
        prosecutingAuthority = ProsecutingAuthority.TFL;
        caseUrn = generate(prosecutingAuthority);
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), PROSECUTOR_ID);
        stubForUserDetails(user, prosecutingAuthority.name());

        createCasePayloadBuilder = withDefaults()
                .withProsecutingAuthority(prosecutingAuthority)
                .withOffenceBuilders(
                        OffenceBuilder.withDefaults().withId(offenceId1)
                                .withLibraOffenceCode(LIBRA_OFFENCE_CODE1),
                        OffenceBuilder.withDefaults().withId(offenceId2)
                                .withLibraOffenceCode(LIBRA_OFFENCE_CODE2),
                        OffenceBuilder.withDefaults().withId(offenceId3)
                                .withLibraOffenceCode(LIBRA_OFFENCE_CODE1))
                .withDefendantBuilder(DefendantBuilder.withDefaults()
                        .withId(defendantId)
                        .withNationalInsuranceNumber(NATIONAL_INSURANCE_NUMBER)
                        .withHearingLanguage(Language.W))
                .withId(caseId)
                .withUrn(caseUrn);

        stubRegionByPostcode(NATIONAL_COURT_CODE, "DEFENDANT_REGION");
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");

        createCaseForPayloadBuilder(createCasePayloadBuilder);
        pollUntilCaseReady(caseId);
    }

    private ReferForCourtHearing buildReferForCourtHearingDecision(Language language, final NextHearing nextHearing, final UUID... offenceIds) {
        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("French", true), language.equals(W), NO_DISABILITY_NEEDS);
        List<OffenceDecisionInformation> offenceDecisionInformations = of(offenceIds).map(offenceId -> createOffenceDecisionInformation(offenceId, VerdictType.NO_VERDICT)).collect(toList());
        return new ReferForCourtHearing(null, offenceDecisionInformations, REFERRAL_REASON_ID, "listing notes", 10, defendantCourtOptions, nextHearing);
    }

    private ReferForCourtHearing buildReferForCourtHearingDecision_WithVerdictType(final UUID... offenceIds) {
        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("French", true), Language.W.equals(W), NO_DISABILITY_NEEDS);
        List<OffenceDecisionInformation> offenceDecisionInformations = of(offenceIds).map(offenceId -> createOffenceDecisionInformation(offenceId, VerdictType.PROVED_SJP)).collect(toList());
        return new ReferForCourtHearing(null, offenceDecisionInformations, REFERRAL_REASON_ID, "listing notes", 10, defendantCourtOptions, null);
    }

    private ReferForCourtHearing buildReferForCourtHearingDecision(final PressRestriction pressRestriction, final UUID... offenceIds) {
        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("French", true), Language.W.equals(W), NO_DISABILITY_NEEDS);
        List<OffenceDecisionInformation> offenceDecisionInformations = of(offenceIds).map(offenceId -> createOffenceDecisionInformation(offenceId, VerdictType.NO_VERDICT)).collect(toList());
        return new ReferForCourtHearing(null, offenceDecisionInformations, REFERRAL_REASON_ID, "reason", "listing notes", 10, defendantCourtOptions, pressRestriction, null);
    }

    private void addCaseDocumentAndUpdateEmployer() {
        new CaseDocumentHelper(caseId).addCaseDocument(USER_ID, DOCUMENT_ID, MATERIAL_ID, DOCUMENT_TYPE);
        pollForCase(caseId, new Matcher[]{withJsonPath("$.caseDocuments[0].id", is(DOCUMENT_ID.toString()))});

        new EmployerHelper().updateEmployer(caseId, defendantId.toString(), employerDetails);
        pollForEmployerForDefendant(defendantId.toString(), isJson(withJsonPath("$.employeeReference", is(employeeReference))));
    }

    private ReferForCourtHearing buildReferForCourtHearingWithDisaNeededDecision(final UUID... offenceIds) {
        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("French", true), Language.W.equals(W), disabilityNeedsOf(DISABILITY_NEEDS));
        List<OffenceDecisionInformation> offenceDecisionInformations = of(offenceIds).map(offenceId -> createOffenceDecisionInformation(offenceId, VerdictType.NO_VERDICT)).collect(toList());
        return new ReferForCourtHearing(null, offenceDecisionInformations, REFERRAL_REASON_ID, "listing notes", 10, defendantCourtOptions, null);
    }

    private NextHearing getNextHearingWithSlots() {
        return nextHearing()
                .withType(hearingType().withId(fromString("bf8155e1-90b9-4080-b133-bfbad895d6e4")).withDescription("Trial").build())
                .withEstimatedMinutes(20)
                .withListedStartDateTime(listedStartDateTime)
                .withCourtCentre(courtCentre()
                        .withId(fromString("4b6185e1-92f2-3634-b749-87d2fcadf1c8"))
                        .withName("Basingstoke Magistrates' Court").withRoomId(roomId).build())
                .withHmiSlots(singletonList(RotaSlot.rotaSlot()
                        .withStartTime(listedStartDateTime)
                        .withDuration(30)
                        .withSession("session")
                        .withOucode("oucode")
                        .withCourtRoomId(1234)
                        .withCourtCentreId("courtCentreId")
                        .withRoomId("roomId")
                        .build()))
                .build();
    }
}
