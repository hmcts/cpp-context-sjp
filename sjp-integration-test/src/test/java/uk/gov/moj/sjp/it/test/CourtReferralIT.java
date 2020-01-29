package uk.gov.moj.sjp.it.test;

import static com.jayway.awaitility.Awaitility.await;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.justice.json.schemas.domains.sjp.Language.E;
import static uk.gov.justice.json.schemas.domains.sjp.Language.W;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseReferralHelper.findReferralStatusForCase;
import static uk.gov.moj.sjp.it.helper.PleadOnlineHelper.verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.MaterialStub.stubMaterialMetadata;
import static uk.gov.moj.sjp.it.stub.ProgressionServiceStub.stubReferCaseToCourtCommand;
import static uk.gov.moj.sjp.it.stub.ProgressionServiceStub.verifyReferToCourtCommandSent;
import static uk.gov.moj.sjp.it.stub.ProsecutionCaseFileServiceStub.stubCaseDetails;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCountryNationalities;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEthnicities;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubHearingTypesQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryOffencesByCode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralDocumentMetadataQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubReferralReasonsQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.DateUtils.CPP_ZONED_DATE_TIME_FORMATTER;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.json.schemas.domains.sjp.Language;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.event.EmployerUpdated;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseCourtReferralStatus;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EmployerHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.PleadOnlineHelper;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.producer.ReferToCourtHearingProducer;
import uk.gov.moj.sjp.it.util.FileUtil;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;

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
    private static final ZonedDateTime ADDED_AT = now(UTC);
    private static final String REFERENCE_DATA_DOCUMENT_TYPE = "Case Summary";

    private static final String NATIONAL_INSURANCE_NUMBER = "BB333333B";
    private static final JsonObject EMPLOYER_DETAILS = createEmployerDetails();

    private static final User user = new User("John", "Smith", USER_ID);

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

    @Before
    public void setUp() throws Exception {
        sessionId = randomUUID();
        caseId = randomUUID();
        offenceId1 = randomUUID();
        offenceId2 = randomUUID();
        offenceId3 = randomUUID();
        defendantId = randomUUID();

        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubAssignmentReplicationCommands();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubQueryOffencesByCode(LIBRA_OFFENCE_CODE2);
        stubReferralReasonsQuery(REFERRAL_REASON_ID, HEARING_CODE, REFERRAL_REASON);
        stubHearingTypesQuery(HEARING_TYPE_ID.toString(), HEARING_CODE, HEARING_DESCRIPTION);
        stubCountryNationalities("stub-data/referencedata.query.country-nationality.json");
        stubEthnicities("stub-data/referencedata.query.ethnicities.json");
        stubReferralDocumentMetadataQuery(DOCUMENT_TYPE_ID.toString(), REFERENCE_DATA_DOCUMENT_TYPE);
        stubMaterialMetadata(MATERIAL_ID, FILE_NAME, MIME_TYPE, ADDED_AT);
        stubReferCaseToCourtCommand();

        new SjpDatabaseCleaner().cleanAll();
    }

    @Test
    public void shouldRecordCaseReferralRejection() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-welsh.json");
        createCaseWithSingleOffence(W);
        addCaseDocumentAndUpdateEmployer();

        final String referralRejectionReason = "Test referral rejection reason";

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);

        final ReferToCourtHearingProducer referToCourtHearingProducer = new ReferToCourtHearingProducer(caseId, REFERRAL_REASON_ID, HEARING_TYPE_ID, referralRejectionReason);

        final ReferForCourtHearing referForCourtHearingDecision = buildReferForCourtHearingDecision(W, offenceId1);

        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, asList(referForCourtHearingDecision), null);

        final String rejectionRecordedEventName = CaseReferralForCourtHearingRejectionRecorded.class.getAnnotation(Event.class).value();

        final Optional<JsonEnvelope> hearingRejectionRecordedEvent = new EventListener()
                .subscribe(CaseReferredForCourtHearing.EVENT_NAME)
                .subscribe(rejectionRecordedEventName)
                .run(() -> DecisionHelper.saveDecision(decision))
                .run(referToCourtHearingProducer::rejectCaseReferral)
                .popEvent(rejectionRecordedEventName);

        assertThat(hearingRejectionRecordedEvent.isPresent(), is(true));

        final CaseCourtReferralStatus referralStatus = await()
                .until(() -> findReferralStatusForCase(caseId), hasProperty("rejectedAt", notNullValue()));

        assertThat(referralStatus.getRequestedAt(), notNullValue());
        assertThat(referralStatus.getRejectedAt(), notNullValue());
        assertThat(referralStatus.getRejectionReason(), is(referralRejectionReason));
        assertThat(referralStatus.getUrn(), is(caseUrn));
    }

    @Test
    public void shouldSendReferToCourtHearingCommandToProgressionContext_NoPlea() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-welsh.json");
        createCaseWithSingleOffence(W);
        addCaseDocumentAndUpdateEmployer();

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);
        referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected("payload/referral/progression.refer-for-court-hearing_no-plea.json", W);
    }

    @Test
    public void shouldSendReferToCourtHearingCommandToProgressionContext_WithPlea() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details.json");
        createCaseWithSingleOffence(E);
        addCaseDocumentAndUpdateEmployer();

        final PleadOnlineHelper pleadOnlineHelper = new PleadOnlineHelper(caseId);
        pleadOnlineHelper.pleadOnline(getPayload("raml/json/sjp.command.plead-online__guilty_request_hearing.json")
                .replace("ecf30a03-8a17-4fc5-81d2-b72ac0a13d17", offenceId1.toString())
                .replace("AB123456A", NATIONAL_INSURANCE_NUMBER));

        verifyOnlinePleaReceivedAndUpdatedCaseDetailsFlag(caseId, true);

        startSessionAndRequestAssignment(sessionId, DELEGATED_POWERS, DEFAULT_LONDON_COURT_HOUSE_OU_CODE);
        referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected("payload/referral/progression.refer-for-court-hearing_plea-present.json", E);
    }

    @Test
    public void shouldReferMultipleOffencesForCourtHearing() {
        stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details-welsh.json");
        createCaseWithMultipleOffences(W);
        addCaseDocumentAndUpdateEmployer();

        final ReferForCourtHearing referForCourtHearingDecision = buildReferForCourtHearingDecision(W, offenceId1, offenceId2);
        final Dismiss dismissDecision = new Dismiss(null, createOffenceDecisionInformation(offenceId3, VerdictType.FOUND_NOT_GUILTY));

        startSessionAndRequestAssignment(sessionId, MAGISTRATE, DEFAULT_NON_LONDON_COURT_HOUSE_OU_CODE);

        final List<OffenceDecision> offencesDecisions = asList(referForCourtHearingDecision, dismissDecision);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, offencesDecisions, null);

        new EventListener()
                .subscribe(CaseReferredForCourtHearing.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final JsonObject expectedCommandPayload = prepareExpectedCommandPayload("payload/referral/progression.refer-for-court-hearing_multiple-offences.json", W);

        verifyReferToCourtCommandSent(expectedCommandPayload);
    }

    private void referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected(String expectedCommandPayloadFile, Language language) {
        final ReferForCourtHearing referForCourtHearingDecision = buildReferForCourtHearingDecision(language, offenceId1);
        final DecisionCommand decision = new DecisionCommand(sessionId, caseId, null, user, asList(referForCourtHearingDecision), null);

        new EventListener()
                .subscribe(CaseReferredForCourtHearing.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decision));

        final JsonObject expectedCommandPayload = prepareExpectedCommandPayload(expectedCommandPayloadFile, language);

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
                .build());
    }

    private static JsonObject createEmployerDetails() {
        final JsonObject address = createObjectBuilder()
                .add("address1", "Foo")
                .add("address2", "Flat 8")
                .add("address3", "Lant House")
                .add("address4", "London")
                .add("address5", "Greater London")
                .add("postcode", "SE1 1PJ").build();

        return createObjectBuilder()
                .add("name", "Test Org")
                .add("employeeReference", "fooo")
                .add("phone", "02020202020")
                .add("address", address).build();
    }

    private static JsonObject startSessionAndRequestAssignment(final UUID sessionId, final SessionType sessionType, final String courtHouseCode) {
        final JsonEnvelope session = startSession(sessionId, USER_ID, courtHouseCode, sessionType).get();
        requestCaseAssignment(sessionId, USER_ID);
        return session.payloadAsJsonObject();
    }

    private void createCaseWithSingleOffence(Language language) {
        prosecutingAuthority = ProsecutingAuthority.TFL;
        caseUrn = generate(prosecutingAuthority);
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), PROSECUTOR_ID);
        stubForUserDetails(user, prosecutingAuthority.name());

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withProsecutingAuthority(prosecutingAuthority)
                .withOffenceBuilder(CreateCase.OffenceBuilder.withDefaults()
                        .withId(offenceId1)
                        .withLibraOffenceCode(LIBRA_OFFENCE_CODE1))
                .withDefendantBuilder(CreateCase.DefendantBuilder.withDefaults()
                        .withId(defendantId)
                        .withNationalInsuranceNumber(NATIONAL_INSURANCE_NUMBER)
                        .withHearingLanguage(language))
                .withId(caseId)
                .withUrn(caseUrn);

        stubRegionByPostcode(NATIONAL_COURT_CODE, "DEFENDANT_REGION");
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder))
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);
    }

    private void createCaseWithMultipleOffences(Language language) {
        prosecutingAuthority = ProsecutingAuthority.TVL;
        caseUrn = generate(prosecutingAuthority);
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), PROSECUTOR_ID);
        stubForUserDetails(user, prosecutingAuthority.name());

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withProsecutingAuthority(prosecutingAuthority)
                .withOffenceBuilders(
                        CreateCase.OffenceBuilder.withDefaults().withId(offenceId1)
                                .withLibraOffenceCode(LIBRA_OFFENCE_CODE1),
                        CreateCase.OffenceBuilder.withDefaults().withId(offenceId2)
                                .withLibraOffenceCode(LIBRA_OFFENCE_CODE2),
                        CreateCase.OffenceBuilder.withDefaults().withId(offenceId3)
                                .withLibraOffenceCode(LIBRA_OFFENCE_CODE1))
                .withDefendantBuilder(CreateCase.DefendantBuilder.withDefaults()
                        .withId(defendantId)
                        .withNationalInsuranceNumber(NATIONAL_INSURANCE_NUMBER)
                        .withHearingLanguage(language))
                .withId(caseId)
                .withUrn(caseUrn);

        stubRegionByPostcode(NATIONAL_COURT_CODE, "DEFENDANT_REGION");
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");

        new EventListener()
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder))
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);
    }

    private ReferForCourtHearing buildReferForCourtHearingDecision(Language language, final UUID... offenceIds) {
        final DefendantCourtOptions defendantCourtOptions = new DefendantCourtOptions(new DefendantCourtInterpreter("French", true), language.equals(W));
        List<OffenceDecisionInformation> offenceDecisionInformations = of(offenceIds).map(offenceId -> createOffenceDecisionInformation(offenceId, VerdictType.NO_VERDICT)).collect(toList());
        return new ReferForCourtHearing(null, offenceDecisionInformations, REFERRAL_REASON_ID, "listing notes", 10, defendantCourtOptions);
    }

    private void addCaseDocumentAndUpdateEmployer() {
        new EventListener()
                .subscribe(CaseDocumentAdded.EVENT_NAME, EmployerUpdated.EVENT_NAME)
                .run(() -> new CaseDocumentHelper(caseId).addCaseDocument(USER_ID, DOCUMENT_ID, MATERIAL_ID, DOCUMENT_TYPE))
                .run(() -> new EmployerHelper().updateEmployer(caseId, defendantId.toString(), EMPLOYER_DETAILS));
    }
}
