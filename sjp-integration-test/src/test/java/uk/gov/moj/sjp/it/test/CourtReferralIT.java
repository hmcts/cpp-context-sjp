package uk.gov.moj.sjp.it.test;

import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.helper.CaseReferralHelper.findReferralStatusForCase;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.stub.ProgressionServiceStub.REFER_TO_COURT_COMMAND_CONTENT;
import static uk.gov.moj.sjp.it.stub.ProgressionServiceStub.REFER_TO_COURT_COMMAND_URL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.util.FileUtil.getPayload;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.CaseDocumentAdded;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.CaseReferralForCourtHearingRejectionRecorded;
import uk.gov.moj.cpp.sjp.event.CaseReferredForCourtHearing;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseCourtReferralStatus;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CaseDocumentHelper;
import uk.gov.moj.sjp.it.helper.EmployerHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.PleadOnlineHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;
import uk.gov.moj.sjp.it.producer.CompleteCaseProducer;
import uk.gov.moj.sjp.it.producer.DecisionToReferCaseForCourtHearingSavedProducer;
import uk.gov.moj.sjp.it.producer.ReferToCourtHearingProducer;
import uk.gov.moj.sjp.it.stub.AssignmentStub;
import uk.gov.moj.sjp.it.stub.MaterialStub;
import uk.gov.moj.sjp.it.stub.ProgressionServiceStub;
import uk.gov.moj.sjp.it.stub.ProsecutionCaseFileServiceStub;
import uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub;
import uk.gov.moj.sjp.it.stub.ResultingStub;
import uk.gov.moj.sjp.it.stub.SchedulingStub;
import uk.gov.moj.sjp.it.stub.UsersGroupsStub;
import uk.gov.moj.sjp.it.util.FileUtil;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import javax.json.JsonObject;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.CoreMatchers;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class CourtReferralIT extends BaseIntegrationTest {

    private static final String SJP_EVENTS_CASE_REFERRED_FOR_COURT_HEARING = "sjp.events.case-referred-for-court-hearing";
    private static final String LONDON_COURT_HOUSE_OU_CODE = "B01OK";

    private static final UUID REFERRAL_REASON_ID = randomUUID();
    private static final String REFERRAL_REASON = "referral reason";
    private static final ZonedDateTime RESULTED_ON = now(UTC);

    private static final UUID HEARING_TYPE_ID = fromString("06b0c2bf-3f98-46ed-ab7e-56efaf9ecced");
    private static final String HEARING_DESCRIPTION = "Plea & Trial Preparation";

    private static final Integer ESTIMATED_HEARING_DURATION = 10;
    private static final String LISTING_NOTES = randomAlphanumeric(100);

    private static final UUID PROSECUTOR_ID = randomUUID();
    private static final String LIBRA_OFFENCE_CODE = "PS90010";

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
    private static final EmployerHelper EMPLOYER_HELPER = new EmployerHelper();

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;
    private String defendantId;
    private String prosecutingAuthorityName;
    private String caseUrn;

    private UUID sessionId;
    private UUID caseId;
    private UUID offenceId;

    @Before
    public void setUp() {
        sessionId = randomUUID();
        caseId = randomUUID();
        offenceId = randomUUID();

        createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults()
                .withOffenceId(offenceId)
                .withOffenceBuilder(CreateCase.OffenceBuilder.withDefaults()
                        .withId(offenceId)
                        .withLibraOffenceCode(LIBRA_OFFENCE_CODE))
                .withDefendantBuilder(CreateCase.DefendantBuilder.withDefaults()
                        .withNationalInsuranceNumber(NATIONAL_INSURANCE_NUMBER))
                .withId(caseId);

        final EventListener eventListener = new EventListener();
        eventListener
                .subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder))
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);

        defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
        prosecutingAuthorityName = createCasePayloadBuilder.getProsecutingAuthority().name();
        caseUrn = createCasePayloadBuilder.getUrn();

        stubCourtByCourtHouseOUCodeQuery(LONDON_COURT_HOUSE_OU_CODE, "2572");
        startSession(sessionId, USER_ID, LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);

        new EventListener()
                .subscribe(CaseDocumentAdded.EVENT_NAME)
                .run(() -> new CaseDocumentHelper(caseId).addCaseDocument(USER_ID, DOCUMENT_ID, MATERIAL_ID, DOCUMENT_TYPE))
                .popEvent(CaseDocumentAdded.EVENT_NAME);

        EMPLOYER_HELPER.updateEmployer(caseId, defendantId, EMPLOYER_DETAILS);

        AssignmentStub.stubAddAssignmentCommand();
        AssignmentStub.stubRemoveAssignmentCommand();
        SchedulingStub.stubStartSjpSessionCommand();
        ReferenceDataServiceStub.stubReferralReasonsQuery(REFERRAL_REASON_ID.toString(), REFERRAL_REASON);
        ReferenceDataServiceStub.stubHearingTypesQuery(HEARING_TYPE_ID.toString(), HEARING_DESCRIPTION);
        ReferenceDataServiceStub.stubProsecutorQuery(prosecutingAuthorityName, PROSECUTOR_ID);
        ReferenceDataServiceStub.stubQueryOffences("stub-data/referencedata.query.offences.json");
        ReferenceDataServiceStub.stubCountryNationalities("stub-data/referencedata.query.country-nationality.json");
        ReferenceDataServiceStub.stubEthnicities("stub-data/referencedata.query.ethnicities.json");
        ReferenceDataServiceStub.stubReferralDocumentMetadataQuery(DOCUMENT_TYPE_ID.toString(), REFERENCE_DATA_DOCUMENT_TYPE);
        ResultingStub.stubGetCaseDecisionsWithDecision(caseId);
        UsersGroupsStub.stubForUserDetails(USER_ID);
        MaterialStub.stubMaterialMetadata(MATERIAL_ID, FILE_NAME, MIME_TYPE, ADDED_AT);
        ProgressionServiceStub.stubReferCaseToCourtCommand();
        ProsecutionCaseFileServiceStub.stubCaseDetails(caseId, "stub-data/prosecutioncasefile.query.case-details.json");
    }

    @Test
    public void shouldReferCaseForCourtHearing() {
        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        final DecisionToReferCaseForCourtHearingSavedProducer decisionToReferCaseForCourtHearingSavedProducer = new DecisionToReferCaseForCourtHearingSavedProducer(
                caseId,
                sessionId,
                REFERRAL_REASON_ID,
                HEARING_TYPE_ID,
                ESTIMATED_HEARING_DURATION,
                LISTING_NOTES,
                RESULTED_ON);

        final Optional<JsonEnvelope> caseReferredForCourtHearingEnvelope = new EventListener()
                .subscribe(CaseReferredForCourtHearing.class.getAnnotation(Event.class).value())
                .run(completeCaseProducer::completeCase)
                .run(decisionToReferCaseForCourtHearingSavedProducer::saveDecisionToReferCaseForCourtHearing)
                .popEvent(SJP_EVENTS_CASE_REFERRED_FOR_COURT_HEARING);

        assertCaseReferredForCourtHearingEventContentsMatch(caseReferredForCourtHearingEnvelope);
        assertReferralRecordedInCourtReferralStatus();
    }

    @Test
    public void shouldRecordCaseReferralRejection() {

        final String referralRejectionReason = "Test referral rejection reason";
        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        final ReferToCourtHearingProducer referToCourtHearingProducer = new ReferToCourtHearingProducer(caseId, REFERRAL_REASON_ID, HEARING_TYPE_ID, referralRejectionReason);
        final DecisionToReferCaseForCourtHearingSavedProducer decisionToReferCaseForCourtHearingSavedProducer = new DecisionToReferCaseForCourtHearingSavedProducer(
                caseId,
                sessionId,
                REFERRAL_REASON_ID,
                HEARING_TYPE_ID,
                ESTIMATED_HEARING_DURATION,
                LISTING_NOTES,
                RESULTED_ON);

        final String rejectionRecordedEventName = CaseReferralForCourtHearingRejectionRecorded.class.getAnnotation(Event.class).value();
        final Optional<JsonEnvelope> hearingRejectionRecordedEvent = new EventListener()
                .subscribe(rejectionRecordedEventName)
                .run(completeCaseProducer::completeCase)
                .run(decisionToReferCaseForCourtHearingSavedProducer::saveDecisionToReferCaseForCourtHearing)
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
        referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected("payload/referral/progression.refer-for-court-hearing_no-plea.json");
    }

    @Test
    public void shouldSendReferToCourtHearingCommandToProgressionContext_WithPlea() {
        new PleadOnlineHelper(caseId).pleadOnline(getPayload("raml/json/sjp.command.plead-online__not-guilty.json")
                .replace("ecf30a03-8a17-4fc5-81d2-b72ac0a13d17", offenceId.toString())
                .replace("AB123456A", NATIONAL_INSURANCE_NUMBER));

        referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected("payload/referral/progression.refer-for-court-hearing_plea-present.json");
    }

    private void referCaseToCourtAndVerifyCommandSendToProgressionMatchesExpected(String expectedCommandPayloadFile) {
        final CompleteCaseProducer completeCaseProducer = new CompleteCaseProducer(caseId);
        final DecisionToReferCaseForCourtHearingSavedProducer decisionToReferCaseForCourtHearingSavedProducer = new DecisionToReferCaseForCourtHearingSavedProducer(
                caseId,
                sessionId,
                REFERRAL_REASON_ID,
                HEARING_TYPE_ID,
                ESTIMATED_HEARING_DURATION,
                LISTING_NOTES,
                RESULTED_ON);

        new EventListener()
                .subscribe(CaseReferredForCourtHearing.class.getAnnotation(Event.class).value())
                .run(completeCaseProducer::completeCase)
                .run(decisionToReferCaseForCourtHearingSavedProducer::saveDecisionToReferCaseForCourtHearing);

        final JsonObject expectedCommandPayload = prepareExpectedCommandPayload(expectedCommandPayloadFile);
        final Predicate<JSONObject> commandPayloadPredicate = commandPayload -> commandPayload.toString().equals(expectedCommandPayload.toString());

        await().until(() ->
                findAll(postRequestedFor(urlPathMatching(REFER_TO_COURT_COMMAND_URL + ".*"))
                        .withHeader(CONTENT_TYPE, WireMock.equalTo(REFER_TO_COURT_COMMAND_CONTENT)))
                        .stream()
                        .map(LoggedRequest::getBodyAsString)
                        .map(JSONObject::new)
                        .anyMatch(commandPayloadPredicate));
    }

    private JsonObject prepareExpectedCommandPayload(String payloadFileLocation) {
        return FileUtil.getFileContentAsJson(payloadFileLocation, ImmutableMap.<String, Object>builder()
                .put("OFFENCE_ID", offenceId.toString())
                .put("CASE_ID", caseId.toString())
                .put("DEFENDANT_ID", defendantId)
                .put("PROSECUTING_AUTHORITY_REFERENCE", caseUrn)
                .put("PROSECUTING_AUTHORITY_ID", PROSECUTOR_ID.toString())
                .put("HEARING_TYPE_ID", HEARING_TYPE_ID.toString())
                .put("REFERRAL_REASON_ID", REFERRAL_REASON_ID.toString())
                .put("LISTING_NOTES", LISTING_NOTES)
                .put("CONVICTION_DATE", RESULTED_ON.toLocalDate().toString())
                .put("MAGISTRATE_ID", sessionId.toString())
                .put("PLEA_DATE", LocalDate.now().toString())
                .put("REFERRAL_DATE", LocalDate.now().toString())
                .put("DOCUMENT_ID", DOCUMENT_ID.toString())
                .put("DOCUMENT_TYPE_ID", DOCUMENT_TYPE_ID.toString())
                .put("MATERIAL_ID", MATERIAL_ID.toString())
                .put("UPLOAD_DATE_TIME", ADDED_AT.format(DateTimeFormatter.ISO_ZONED_DATE_TIME))
                .put("NINO", NATIONAL_INSURANCE_NUMBER)
                .build());
    }

    private void assertReferralRecordedInCourtReferralStatus() {
        final CaseCourtReferralStatus referralStatus = await()
                .until(
                        () -> findReferralStatusForCase(caseId),
                        notNullValue());

        assertThat(referralStatus.getRequestedAt(), notNullValue());
        assertThat(referralStatus.getRejectedAt(), nullValue());
        assertThat(referralStatus.getRejectionReason(), nullValue());
    }

    private void assertCaseReferredForCourtHearingEventContentsMatch(final Optional<JsonEnvelope> caseReferredForCourtHearingEnvelope) {
        assertThat(caseReferredForCourtHearingEnvelope.isPresent(), is(true));

        assertThat(caseReferredForCourtHearingEnvelope.get(),
                jsonEnvelope(
                        metadata().withName(SJP_EVENTS_CASE_REFERRED_FOR_COURT_HEARING),
                        payload().isJson(allOf(
                                withJsonPath("$.caseId", CoreMatchers.equalTo(caseId.toString())),
                                withJsonPath("$.sessionId", CoreMatchers.equalTo(sessionId.toString())),
                                withJsonPath("$.referralReasonId", CoreMatchers.equalTo(REFERRAL_REASON_ID.toString())),
                                withJsonPath("$.hearingTypeId", CoreMatchers.equalTo(HEARING_TYPE_ID.toString())),
                                withJsonPath("$.estimatedHearingDuration", CoreMatchers.equalTo(ESTIMATED_HEARING_DURATION)),
                                withJsonPath("$.listingNotes", CoreMatchers.equalTo(LISTING_NOTES)),
                                withJsonPath("$.referredAt", CoreMatchers.equalTo(RESULTED_ON.toString()))
                        ))));
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
}
