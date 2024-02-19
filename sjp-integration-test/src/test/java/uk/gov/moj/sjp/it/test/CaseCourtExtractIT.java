package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.time.Month.JULY;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.Discharge.createDischarge;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.CONDITIONAL;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.PeriodUnit.MONTH;
import static uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentType.PAY_TO_COURT;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.createCaseApplication;
import static uk.gov.moj.sjp.it.helper.CaseApplicationHelper.saveApplicationDecision;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyCaseCompleted;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.setPleas;
import static uk.gov.moj.sjp.it.model.PleaInfo.pleaInfo;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubFixedLists;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForAllProsecutors;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubQueryForVerdictTypes;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultDefinitions;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubResultIds;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SystemDocumentGeneratorStub.pollDocumentGenerationRequests;
import static uk.gov.moj.sjp.it.stub.SystemDocumentGeneratorStub.stubDocumentGeneratorEndPoint;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;
import static uk.gov.moj.sjp.it.util.QueueUtil.sendToQueue;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.CourtDetails;
import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.CostsAndSurcharge;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.InstallmentPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Installments;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.Payment;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.PaymentTerms;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.CaseCompleted;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper;
import uk.gov.moj.sjp.it.util.JsonHelper;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.util.builders.DismissBuilder;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class CaseCourtExtractIT extends BaseIntegrationTest {

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MMMM yyyy");
    private static final DateTimeFormatter DATE_FORMAT_INSTALMENTS = ofPattern("d MMMM yyyy");
    private final UUID caseId = randomUUID();
    private UUID appId = randomUUID();
    private final UUID offenceId = UUID.fromString("7884634a-8b25-4650-be3b-7ca393309001");
    private final UUID offence2d = UUID.fromString("7884634a-8b25-4650-be3b-7ca393309002");
    private final UUID defendantId = randomUUID();
    private final String legalEntityName = "Samba LTD";
    private final UUID magistrateSessionId = randomUUID();
    private final UUID delegatedPowersSessionId = randomUUID();
    private final User user = new User("John", "Smith", USER_ID);
    private final EventListener eventListener = new EventListener();
    private final String courtExtract = "Court extract payload generated at " + now();
    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private final ProsecutingAuthority prosecutingAuthority = TFL;
    private final String urn = generate(prosecutingAuthority);
    private final LocalDate defendantDateOfBirth = LocalDate.of(1980, JULY, 15);
    private final int adjournmentPeriod = 1;
    private static final String DEFENDANT_REGION = "croydon";
    private static final String NATIONAL_COURT_CODE = "1080";
    private final String SJP_EVENT_CASE_APP_RECORDED = "sjp.events.case-application-recorded";
    private final String SJP_EVENT_CASE_APP_STAT_DEC = "sjp.events.case-stat-dec-recorded";
    private final String SJP_EVENT_APPLICATION_DECISION_SAVED = "sjp.events.application-decision-saved";
    private static final String createCaseApplicationFile = "CaseApplicationIT/sjp.command.create-case-application.json";
    private static final UUID STAT_DEC_TYPE_ID = fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e");
    private static final String STAT_DEC_TYPE_CODE = "MC80528";
    private static final String APP_STATUS = "DRAFT";
    private final static LocalDate DATE_RECEIVED = LocalDate.now().minusDays(7);

    @Before
    public void setUp() throws SQLException {
        databaseCleaner.cleanViewStore();
        WireMock.resetAllRequests();
        stubFixedLists();
        stubAllResultDefinitions();
        stubQueryForVerdictTypes();
        stubQueryForAllProsecutors();
        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));
        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubDocumentGeneratorEndPoint(courtExtract.getBytes());
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubForUserDetails(user, "ALL");

        CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        stubRegionByPostcode(NATIONAL_COURT_CODE, DEFENDANT_REGION);
        stubResultDefinitions();
        stubResultIds();

    }

    @Test
    public void shouldGenerateCourtExtract() {
        final CreateCase.CreateCasePayloadBuilder caseBuilder = CreateCase
                .CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withOffenceBuilders(CreateCase.OffenceBuilder.withDefaults().withId(offenceId),
                        CreateCase.OffenceBuilder.withDefaults().withId(offence2d))
                .withOffenceCode(DEFAULT_OFFENCE_CODE)
                .withUrn(urn);
        createCaseAndWaitUntilReady(caseBuilder);
        stubEnforcementAreaByPostcode(caseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");

        setPlea(GUILTY_REQUEST_HEARING);

        adjournCase();

        setPlea(GUILTY);

        expireAdjournment();

        dismissCase();

        waitUntilNewDecision();

        verifyCourtExtractResponse();

        verifyCourtExtractGenerationRequest();
    }

    @Test
    public void shouldGenerateCourtExtractForCompany() {
        final CreateCase.CreateCasePayloadBuilder caseBuilder = CreateCase
                .CreateCasePayloadBuilder
                .withDefaults()
                .withDefendantBuilder(CreateCase.DefendantBuilder.defaultDefendant().withLegalEntityName(legalEntityName))
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withOffenceBuilders(CreateCase.OffenceBuilder.withDefaults().withId(offenceId),
                        CreateCase.OffenceBuilder.withDefaults().withId(offence2d))
                .withOffenceCode(DEFAULT_OFFENCE_CODE)
                .withUrn(urn);
        createCaseAndWaitUntilReady(caseBuilder);
        stubEnforcementAreaByPostcode(caseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        setPlea(GUILTY_REQUEST_HEARING);

        adjournCase();

        setPlea(GUILTY);

        expireAdjournment();

        dismissCase();

        waitUntilNewDecision();

        verifyCourtExtractResponse();

        verifyCourtExtractGenerationRequestForCompany();
    }

    @Test
    public void shouldGenerateCourtExtractWithDecisionPostApplication() {
        final CreateCase.CreateCasePayloadBuilder caseBuilder = CreateCase
                .CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withOffenceBuilders(CreateCase.OffenceBuilder.withDefaults().withId(offenceId),
                        CreateCase.OffenceBuilder.withDefaults().withId(offence2d))
                .withOffenceCode(DEFAULT_OFFENCE_CODE)
                .withUrn(urn);
        createCaseAndWaitUntilReady(caseBuilder);
        stubEnforcementAreaByPostcode(caseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");

        setPlea(GUILTY_REQUEST_HEARING);

        adjournCase();

        setPlea(GUILTY);

        expireAdjournment();

        dismissCase();

        recordApplicationDecision();

        pollUntilCaseReady(caseId);

        dischargeCase();

        verifyCourtExtractResponse();

        verifyCourtExtractGenerationRequestWithApplication();
    }

    @Test
    public void shouldGenerateCourtExtractWithDecisionApplicationAndPaymentAndCollection() {
        final CreateCase.CreateCasePayloadBuilder caseBuilder = CreateCase
                .CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withOffenceBuilders(CreateCase.OffenceBuilder.withDefaults().withId(offenceId),
                        CreateCase.OffenceBuilder.withDefaults().withId(offence2d))
                .withOffenceCode(DEFAULT_OFFENCE_CODE)
                .withUrn(urn);
        createCaseAndWaitUntilReady(caseBuilder);
        stubEnforcementAreaByPostcode(caseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");

        setPlea(GUILTY);

        dischargeCase();

        recordApplicationDecision();

        pollUntilCaseReady(caseId);

        dischargeCase();

        verifyCourtExtractResponse();

        verifyCourtExtractGenerationRequestWithApplicationAndPayment();
    }

    private void verifyCourtExtractResponse() {
        final Response courtExtractResponse = getCaseCourtExtract(caseId, user.getUserId());
        assertThat(courtExtractResponse.getHeaderString(CONTENT_TYPE), equalTo("application/pdf"));
        assertThat(courtExtractResponse.getHeaderString(CONTENT_DISPOSITION), equalTo("attachment; filename=\"court_extract.pdf\""));
        assertThat(courtExtractResponse.readEntity(String.class), equalTo(courtExtract));
    }

    private void verifyCourtExtractGenerationRequest() {


        final JSONObject documentGenerationRequest = pollDocumentGenerationRequests(hasSize(1)).get(0);

        assertThat(documentGenerationRequest.getString("templateName"), is("CourtExtract"));
        assertThat(documentGenerationRequest.getString("conversionFormat"), is("pdf"));

        final JSONObject actualPayload = documentGenerationRequest.getJSONObject("templatePayload");

        final JsonObject expectedPayload = getExpectedJsonObject("CourtExtractIT/system-document-generator-command.json");

        // To avoid flaky tests, the 'generationTime' is not verified
        final JsonObject expectedTemplatePayload = expectedPayload.getJsonObject("templatePayload");

        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonObject("defendant"),
                actualPayload.getJSONObject("defendant")), is(true));
        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonArray("decisionCourtExtractView").getJsonObject(0).getJsonArray("offencesApplicationsDecisions").getJsonObject(0),
                actualPayload.getJSONArray("decisionCourtExtractView").getJSONObject(0).getJSONArray("offencesApplicationsDecisions").getJSONObject(0)), is(true));

        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonObject("caseDetails"),
                actualPayload.getJSONObject("caseDetails")), is(true));
    }

    private void verifyCourtExtractGenerationRequestForCompany() {

        final JSONObject documentGenerationRequest = pollDocumentGenerationRequests(hasSize(1)).get(0);

        assertThat(documentGenerationRequest.getString("templateName"), is("CourtExtract"));
        assertThat(documentGenerationRequest.getString("conversionFormat"), is("pdf"));

        final JSONObject actualPayload = documentGenerationRequest.getJSONObject("templatePayload");
        JsonObject expectedTemplatePayload= null;

        final JsonObject expectedPayload = getExpectedJsonObject("CourtExtractIT/system-document-generator-command-company.json");

        // To avoid flaky tests, the 'generationTime' is not verified

        if(nonNull(expectedPayload)) {
            expectedTemplatePayload = expectedPayload.getJsonObject("templatePayload");
        }
        if(nonNull(expectedTemplatePayload)) {
        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonObject("defendant"),
                actualPayload.getJSONObject("defendant")), is(true));


        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonArray("decisionCourtExtractView").getJsonObject(0).getJsonArray("offencesApplicationsDecisions").getJsonObject(0),
                actualPayload.getJSONArray("decisionCourtExtractView").getJSONObject(0).getJSONArray("offencesApplicationsDecisions").getJSONObject(0)), is(true));

            assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonObject("caseDetails"),
                    actualPayload.getJSONObject("caseDetails")), is(true));
        }
    }

    private void verifyCourtExtractGenerationRequestWithApplication() {
        final JSONObject documentGenerationRequest = pollDocumentGenerationRequests(hasSize(1)).get(0);

        assertThat(documentGenerationRequest.getString("templateName"), is("CourtExtract"));
        assertThat(documentGenerationRequest.getString("conversionFormat"), is("pdf"));

        final JSONObject actualPayload = documentGenerationRequest.getJSONObject("templatePayload");

        final JsonObject expectedPayload = getExpectedJsonObject("CourtExtractIT/system-document-generator-command-case-with-application.json");

        // To avoid flaky tests, the 'generationTime' is not verified
        final JsonObject expectedTemplatePayload = expectedPayload.getJsonObject("templatePayload");

        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonArray("decisionCourtExtractView").getJsonObject(0).getJsonArray("offencesApplicationsDecisions").getJsonObject(0),
                actualPayload.getJSONArray("decisionCourtExtractView").getJSONObject(0).getJSONArray("offencesApplicationsDecisions").getJSONObject(0)), is(true));

        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonArray("decisionCourtExtractView").getJsonObject(0).getJsonArray("offencesApplicationsDecisions").getJsonObject(1),
                actualPayload.getJSONArray("decisionCourtExtractView").getJSONObject(0).getJSONArray("offencesApplicationsDecisions").getJSONObject(1)), is(true));

        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonArray("decisionCourtExtractView").getJsonObject(1).getJsonArray("offencesApplicationsDecisions").getJsonObject(0),
                actualPayload.getJSONArray("decisionCourtExtractView").getJSONObject(1).getJSONArray("offencesApplicationsDecisions").getJSONObject(0)), is(true));

    }

    private void verifyCourtExtractGenerationRequestWithApplicationAndPayment() {
        final JSONObject documentGenerationRequest = pollDocumentGenerationRequests(hasSize(1)).get(0);

        assertThat(documentGenerationRequest.getString("templateName"), is("CourtExtract"));
        assertThat(documentGenerationRequest.getString("conversionFormat"), is("pdf"));

        final JSONObject actualPayload = documentGenerationRequest.getJSONObject("templatePayload");

        final JsonObject expectedPayload = getExpectedJsonObject("CourtExtractIT/system-document-generator-command-case-with-application-and-payment.json");

        // To avoid flaky tests, the 'generationTime' is not verified
        final JsonObject expectedTemplatePayload = expectedPayload.getJsonObject("templatePayload");

        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonArray("decisionCourtExtractView").getJsonObject(0).getJsonObject("payment"),
                actualPayload.getJSONArray("decisionCourtExtractView").getJSONObject(0).getJSONObject("payment")), is(true));

        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonArray("decisionCourtExtractView").getJsonObject(2).getJsonObject("payment"),
                actualPayload.getJSONArray("decisionCourtExtractView").getJSONObject(2).getJSONObject("payment")), is(true));
    }

    private void setPlea(final PleaType pleaType) {
        setPleas(caseId, defendantId, pleaInfo(offenceId, pleaType));
    }

    private void adjournCase() {
        assignCaseInDelegatedPowersSession(delegatedPowersSessionId, user.getUserId());

        final Adjourn adjournDecision = new Adjourn(null, singletonList(new OffenceDecisionInformation(offenceId, NO_VERDICT)), "reason", now().plusDays(adjournmentPeriod));
        final Adjourn adjournDecision2 = new Adjourn(null, singletonList(new OffenceDecisionInformation(offence2d, NO_VERDICT)), "reason", now().plusDays(adjournmentPeriod));

        final DecisionCommand decision = new DecisionCommand(delegatedPowersSessionId, caseId, "Test note", user, asList(adjournDecision, adjournDecision2), null);

        saveDecision(decision);
    }

    private void dismissCase() {
        assignCaseInMagistrateSession(magistrateSessionId, user.getUserId());

        final OffenceDecision offenceDecision = DismissBuilder.withDefaults(offenceId).build();
        final OffenceDecision offenceDecision2 = DismissBuilder.withDefaults(offence2d).build();

        final DecisionCommand decision = new DecisionCommand(magistrateSessionId, caseId, "Test note", user, asList(offenceDecision, offenceDecision2), null);

        saveDecision(decision);
    }

    private void dischargeCase() {
        assignCaseInMagistrateSession(magistrateSessionId, user.getUserId());
        final Discharge discharge = createDischarge(null, createOffenceDecisionInformation(offenceId, FOUND_GUILTY), CONDITIONAL, new DischargePeriod(2, MONTH), new BigDecimal(230), null, false, null, null);
        discharge.getOffenceDecisionInformation().setPressRestrictable(false);
        final Discharge discharge2 = createDischarge(randomUUID(), createOffenceDecisionInformation(offence2d, FOUND_GUILTY), ABSOLUTE, null, new BigDecimal(20), null, true, null, null);

        final DecisionCommand decision = new DecisionCommand(magistrateSessionId, caseId, null, user, asList(discharge, discharge2), financialImposition());
        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .subscribe(CaseCompleted.EVENT_NAME)
                .run(() -> saveDecision(decision));
        final CaseCompleted caseCompleted = eventListener.popEventPayload(CaseCompleted.class);
        verifyCaseCompleted(caseId, caseCompleted);

    }

    private void recordApplicationDecision() {
        eventListener.subscribe(SJP_EVENT_CASE_APP_RECORDED, SJP_EVENT_CASE_APP_STAT_DEC)
                .run(() -> createCaseApplication(user.getUserId(), caseId, appId,
                        STAT_DEC_TYPE_ID, STAT_DEC_TYPE_CODE, "A",DATE_RECEIVED, APP_STATUS,
                        createCaseApplicationFile));
        final Optional<JsonEnvelope> caseApplicationRecordedEnv = eventListener.popEvent(SJP_EVENT_CASE_APP_RECORDED);
        assertThat(caseApplicationRecordedEnv.isPresent(), is(true));
        pollUntilCaseReady(caseId);

        assignCaseInMagistrateSession(magistrateSessionId, user.getUserId());

        requestCaseAssignment(magistrateSessionId, user.getUserId());

        eventListener.subscribe(
                SJP_EVENT_APPLICATION_DECISION_SAVED).run(() -> saveApplicationDecision(user.getUserId(), caseId, appId, magistrateSessionId, true, false, null, null));

        final Optional<JsonEnvelope> applicationDecisionSavedEnv = eventListener.popEvent(SJP_EVENT_APPLICATION_DECISION_SAVED);
        assertThat(applicationDecisionSavedEnv.isPresent(), is(true));

    }

    private void waitUntilNewDecision() {
        pollWithDefaults(getCaseById(caseId)).until(status().is(OK), payload().isJson(allOf(withJsonPath("$.caseDecisions.length()", is(2)))));
    }

    private void saveDecision(final DecisionCommand decisionCommand) {
        eventListener
                .subscribe(DecisionSaved.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decisionCommand));
    }

    private void expireAdjournment() {
        new EventListener().subscribe("sjp.events.case-adjourned-to-later-sjp-hearing-recorded")
                .run(() -> sendToQueue("sjp.handler.command", createEnvelope("sjp.command.record-case-adjournment-to-later-sjp-hearing-elapsed",
                        createObjectBuilder().add("caseId", caseId.toString()).build())));
    }

    private void createCaseAndWaitUntilReady(final CreateCase.CreateCasePayloadBuilder caseBuilder) {
        new EventListener().subscribe(CaseMarkedReadyForDecision.EVENT_NAME)
                .run(() -> createCaseForPayloadBuilder(caseBuilder))
                .popEvent(CaseMarkedReadyForDecision.EVENT_NAME);
    }

    private static void assignCaseInMagistrateSession(final UUID sessionId, final UUID userId) {
        startSession(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignment(sessionId, USER_ID);
    }

    private void assignCaseInDelegatedPowersSession(final UUID sessionId, final UUID userId) {
        startSession(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, DELEGATED_POWERS);
        requestCaseAssignment(sessionId, USER_ID);
    }

    private static Response getCaseCourtExtract(final UUID caseId, final UUID userId) {
        final String acceptHeader = "application/vnd.sjp.query.case-court-extract+json";
        final String url = String.format("/cases/%s/documents/court-extract", caseId);
        return makeGetCall(url, acceptHeader, userId);
    }

    private FinancialImposition financialImposition() {
        return new FinancialImposition(
                new CostsAndSurcharge(new BigDecimal(120), null, new BigDecimal(32), null, null, true),
                new Payment(new BigDecimal(272), PAY_TO_COURT, "No information from defendant", null,
                        new PaymentTerms(false, null, new Installments(new BigDecimal(20), InstallmentPeriod.MONTHLY, LocalDate.now().plusDays(30))), (new CourtDetails(NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court"))
                ));
    }

    private JsonObject getExpectedJsonObject(String fileName) {
        return getFileContentAsJson(fileName,
                ImmutableMap.<String, Object>builder()
                        .put("URN", urn)
                        .put("AGE", String.valueOf(Period.between(defendantDateOfBirth, now()).getYears()))
                        .put("DECISION_DATE", now().format(DATE_FORMAT))
                        .put("ADJOURNED_TO", now().plusDays(adjournmentPeriod).format(DATE_FORMAT))
                        .put("PLEA_DATE", now().format(DATE_FORMAT))
                        .put("MAGISTRATE_NAME", "John Smith " + magistrateSessionId)
                        .put("LEGAL_ADVISER_NAME", "John Smith")
                        .put("GENERATION_DATE", now().format(DATE_FORMAT))
                        .put("INSTALLMENT_DATE", now().plusDays(30).format(DATE_FORMAT_INSTALMENTS))
                        .build());
    }
}
