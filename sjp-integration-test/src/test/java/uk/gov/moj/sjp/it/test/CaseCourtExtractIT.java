package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.time.Month.JULY;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.OK;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasLength;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.ResponsePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.matchers.ResponseStatusMatcher.status;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.helper.SetPleasHelper.setPleas;
import static uk.gov.moj.sjp.it.model.PleaInfo.pleaInfo;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubAssignmentReplicationCommands;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SystemDocumentGeneratorStub.pollDocumentGenerationRequests;
import static uk.gov.moj.sjp.it.stub.SystemDocumentGeneratorStub.stubDocumentGeneratorEndPoint;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.sjp.it.util.HttpClientUtil.makeGetCall;
import static uk.gov.moj.sjp.it.util.QueueUtil.sendToQueue;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.pollWithDefaults;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.util.DefaultRequests;
import uk.gov.moj.sjp.it.util.JsonHelper;
import uk.gov.moj.sjp.it.util.RestPollerWithDefaults;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class CaseCourtExtractIT extends BaseIntegrationTest {

    private static final DateTimeFormatter DATE_FORMAT = ofPattern("dd MMMM yyyy");
    private final UUID caseId = randomUUID();
    private final UUID offenceId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final UUID magistrateSessionId = randomUUID();
    private final UUID delegatedPowersSessionId = randomUUID();
    private final UUID prosecutorId = randomUUID();
    private final User user = new User("John", "Smith", USER_ID);
    private final EventListener eventListener = new EventListener();
    private final String courtExtract = "Court extract payload generated at " + now();
    private final SjpDatabaseCleaner databaseCleaner = new SjpDatabaseCleaner();
    private final ProsecutingAuthority prosecutingAuthority = TFL;
    private final String urn = generate(prosecutingAuthority);
    private final LocalDate defendantDateOfBirth = LocalDate.of(1980, JULY, 15);
    private final int adjournmentPeriod = 1;

    @Before
    public void setUp() throws SQLException {
        databaseCleaner.cleanAll();

        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubAssignmentReplicationCommands();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubDocumentGeneratorEndPoint(courtExtract.getBytes());
        stubProsecutorQuery(prosecutingAuthority.name(), randomUUID());
        stubForUserDetails(user);

        final CreateCase.CreateCasePayloadBuilder caseBuilder = CreateCase
                .CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withOffenceId(offenceId)
                .withOffenceCode(DEFAULT_OFFENCE_CODE)
                .withUrn(urn);

        stubEnforcementAreaByPostcode(caseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        createCaseAndWaitUntilReady(caseBuilder);
    }

    @Test
    public void shouldGenerateCourtExtract() {
        setPlea(GUILTY_REQUEST_HEARING);

        adjournCase();

        setPlea(GUILTY);

        expireAdjournment();

        dismissCase();

        waitUntilNewDecision();

        verifyCourtExtractResponse();

        verifyCourtExtractGenerationRequest();
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

        final JsonObject expectedPayload = getFileContentAsJson("CourtExtractIT/system-document-generator-command.json",
                ImmutableMap.<String, Object>builder()
                        .put("URN", urn)
                        .put("AGE", String.valueOf(Period.between(defendantDateOfBirth, now()).getYears()))
                        .put("DECISION_DATE", now().format(DATE_FORMAT))
                        .put("ADJOURNED_TO", now().plusDays(adjournmentPeriod).format(DATE_FORMAT))
                        .put("PLEA_DATE", now().format(DATE_FORMAT))
                        .put("MAGISTRATE_NAME", "John Smith " + magistrateSessionId)
                        .put("LEGAL_ADVISER_NAME", "John Smith " + magistrateSessionId)
                        .put("GENERATION_DATE", now().format(DATE_FORMAT))
                        .build());

        // To avoid flaky tests, the 'generationTime' is not verified
        final JsonObject expectedTemplatePayload = expectedPayload.getJsonObject("templatePayload");

        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonObject("defendant"),
                actualPayload.getJSONObject("defendant")), is(true));
        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonArray("offences").getJsonObject(0),
                actualPayload.getJSONArray("offences").getJSONObject(0)), is(true));
        assertThat(JsonHelper.lenientCompare(expectedTemplatePayload.getJsonObject("caseDetails"),
                actualPayload.getJSONObject("caseDetails")), is(true));
    }

    private void setPlea(final PleaType pleaType) {
        setPleas(caseId, defendantId, pleaInfo(offenceId, pleaType));
    }

    private void adjournCase() {
        assignCaseInDelegatedPowersSession(delegatedPowersSessionId, user.getUserId());

        final OffenceDecision offenceDecision = new Adjourn(null, asList(new OffenceDecisionInformation(offenceId, NO_VERDICT)), "reason", now().plusDays(adjournmentPeriod));
        final DecisionCommand decision = new DecisionCommand(delegatedPowersSessionId, caseId, "Test note", user, asList(offenceDecision), null);

        saveDecision(decision);
    }

    private void dismissCase() {
        assignCaseInMagistrateSession(magistrateSessionId, user.getUserId());

        final OffenceDecision offenceDecision = new Dismiss(null, new OffenceDecisionInformation(offenceId, FOUND_NOT_GUILTY));
        final DecisionCommand decision = new DecisionCommand(magistrateSessionId, caseId, "Test note", user, asList(offenceDecision), null);

        saveDecision(decision);
    }

    private void waitUntilNewDecision(){
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
}
