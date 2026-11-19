package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.Month.JULY;
import static java.util.stream.Collectors.joining;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClientProvider.newPublicJmsMessageProducerClientProvider;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.sjp.it.Constants.DEFAULT_OFFENCE_CODE;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder.withDefaults;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignmentAndConfirm;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.saveDecision;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSessionAndConfirm;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseByIdIsOk;
import static uk.gov.moj.sjp.it.pollingquery.CasePoller.pollUntilCaseStatusCompleted;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.DefaultRequests.getCaseById;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContent;
import static uk.gov.moj.sjp.it.util.FileUtil.getFileContentAsJson;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;
import static uk.gov.moj.sjp.it.util.UrnProvider.generate;

import uk.gov.justice.json.schemas.domains.sjp.ApplicationStatus;
import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.integrationtest.utils.jms.JmsMessageProducerClient;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.http.RequestParams;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.util.JsonHelper;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.builders.DismissBuilder;

import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CCApplicationStatusUpdateIT extends BaseIntegrationTest {

    private final ProsecutingAuthority prosecutingAuthority = TFL;
    private final User user = new User("John", "Smith", USER_ID);
    private final LocalDate defendantDateOfBirth = LocalDate.of(1980, JULY, 15);
    private final String urn = generate(prosecutingAuthority);

    private final UUID magistrateSessionId = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID offenceId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final UUID appId = randomUUID();

    private static final String NATIONAL_COURT_CODE = "1080";
    private static final UUID STAT_DEC_TYPE_ID = fromString("7375727f-30fc-3f55-99f3-36adc4f0e70e");
    private static final String STAT_DEC_TYPE_CODE = "MC80528";
    private static final String REOPENING_TYPE_CODE = "MC80524";

    private final static LocalDate DATE_RECEIVED = LocalDate.now().minusDays(7);

    private static final String STAT_DEC_SJP_TYPE = "Appearance to make statutory declaration (SJP case)";
    private static final String PUBLIC_COURT_APPLICATION_CREATED = "public.progression.court-application-created";
    private static final String PRIVATE_CC_APPLICATION_STATUS_UPDATED = "sjp.events.cc-application-status-updated";

    // result definition ids, see sjp-event-processor ApplicationResult
    private static final String RESULT_GRANTED_G = "2b3f7c20-8fc1-4fad-9076-df196c24b27e";
    private static final String RESULT_DISMISSED_DISM = "14d66587-8fbe-424f-a369-b1144f1684e3";
    // not a final application outcome, so not mapped by SJP
    private static final String RESULT_COSTS = randomUUID().toString();
    private static final String RESULT_ADJOURNMENT = randomUUID().toString();

    private final UUID hearingId = randomUUID();

    @BeforeEach
    public void setUp() throws SQLException {
        cleanViewStore();
        createCase();
        dismissCase();
    }

    @Test
    public void should_createCCApplicationStatus_WhenCCApplicationCreated() {

        final JsonObject payload = getFileContentAsJson("CCApplicationStatusUpdateIT/application-created-in-criminal-courts.json",
                ImmutableMap.<String, Object>builder()
                        .put("CASE_ID", caseId)
                        .put("APP_ID", appId)
                        .put("TYPE_ID", STAT_DEC_TYPE_ID)
                        .put("APPLICATION_CODE", "")
                        .put("APPLICATION_TYPE", "Appeal against Sentence")
                        .put("LINK_TYPE", "SJP")
                        .put("APPLICATION_RECEIVED_DATE", DATE_RECEIVED)
                        .build());
        final JmsMessageProducerClient publicJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                .getMessageProducerClient();
        publicJmsMessageProducerClient.sendMessage("public.progression.court-application-created", payload);


        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.ccApplicationStatus", is(ApplicationStatus.APPEAL_PENDING.name()))));


    }

    @Test
    public void should_createCCApplicationStatus_WhenCCApplicationCreatedWithAppearanceToMakeStatutoryDeclaration() {

        final JsonObject payload = getFileContentAsJson("CCApplicationStatusUpdateIT/application-created-in-criminal-courts.json",
                ImmutableMap.<String, Object>builder()
                        .put("CASE_ID", caseId)
                        .put("APP_ID", appId)
                        .put("TYPE_ID", STAT_DEC_TYPE_ID)
                        .put("APPLICATION_CODE", REOPENING_TYPE_CODE)
                        .put("APPLICATION_TYPE", "Application to reopen case")
                        .put("LINK_TYPE", "LINKED")
                        .put("APPLICATION_RECEIVED_DATE", DATE_RECEIVED)
                        .build());
        final JmsMessageProducerClient publicJmsMessageProducerClient = newPublicJmsMessageProducerClientProvider()
                .getMessageProducerClient();
        publicJmsMessageProducerClient.sendMessage("public.progression.court-application-created", payload);


        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.ccApplicationStatus", is(ApplicationStatus.REOPENING_PENDING.name()))));

    }

    @Test
    public void should_createCCApplicationStatus_WhenCCApplicationToReopenCase() {

        final JsonObject payload = getFileContentAsJson("CCApplicationStatusUpdateIT/application-created-in-criminal-courts.json",
                ImmutableMap.<String, Object>builder()
                        .put("CASE_ID", caseId)
                        .put("APP_ID", appId)
                        .put("TYPE_ID", STAT_DEC_TYPE_ID)
                        .put("APPLICATION_CODE", STAT_DEC_TYPE_CODE)
                        .put("APPLICATION_TYPE", "Appearance to make statutory declaration")
                        .put("LINK_TYPE", "LINKED")
                        .put("APPLICATION_RECEIVED_DATE", DATE_RECEIVED)
                        .build());

        final JmsMessageProducerClient producerClient = newPublicJmsMessageProducerClientProvider()
                .getMessageProducerClient();
        producerClient.sendMessage("public.progression.court-application-created", payload);

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.ccApplicationStatus", is(ApplicationStatus.STATUTORY_DECLARATION_PENDING.name()))));

    }

    @Test
    public void should_updateCCApplicationStatusToGranted_WhenStatDecGrantedAndAncillaryResultSharedFirst() {
        createStatDecApplicationAndWaitUntilPending();

        shareApplicationResults(
                judicialResult(RESULT_COSTS, "Costs", "ANCILLARY", false),
                judicialResult(RESULT_GRANTED_G, "Granted", "FINAL", false));

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.ccApplicationStatus", is(ApplicationStatus.STATUTORY_DECLARATION_GRANTED.name()))));
    }

    @Test
    public void should_keepCCApplicationStatusPending_WhenHearingAdjourned_AndUpdateIt_WhenResultedLater() {
        createStatDecApplicationAndWaitUntilPending();

        // 1st hearing: adjournment only -> NOT_KNOWN is raised but must not overwrite PENDING
        final EventListener eventListener = new EventListener()
                .subscribe(PRIVATE_CC_APPLICATION_STATUS_UPDATED)
                .run(() -> shareApplicationResults(judicialResult(RESULT_ADJOURNMENT, "Adjourned", "INTERMEDIARY", true)));

        final Optional<JsonEnvelope> statusUpdated = eventListener.popEvent(PRIVATE_CC_APPLICATION_STATUS_UPDATED);
        assertThat(statusUpdated.isPresent(), is(true));
        assertThat(statusUpdated.get().payloadAsJsonObject().getString("status"), is(ApplicationStatus.APPLICATION_STATUS_NOT_KNOWN.name()));

        await().pollDelay(Duration.ZERO)
                .during(Duration.ofSeconds(5))
                .atMost(Duration.ofSeconds(10))
                .until(() -> currentCCApplicationStatus(), is(ApplicationStatus.STATUTORY_DECLARATION_PENDING.name()));

        // 2nd hearing: granted -> final status replaces PENDING
        shareApplicationResults(judicialResult(RESULT_GRANTED_G, "Granted", "FINAL", false));

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.ccApplicationStatus", is(ApplicationStatus.STATUTORY_DECLARATION_GRANTED.name()))));
    }

    @Test
    public void should_updateCCApplicationStatusToDismissed_WhenStatDecDismissed() {
        createStatDecApplicationAndWaitUntilPending();

        shareApplicationResults(judicialResult(RESULT_DISMISSED_DISM, "Dismissed", "FINAL", false));

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.ccApplicationStatus", is(ApplicationStatus.STATUTORY_DECLARATION_DISMISSED.name()))));
    }

    private void createStatDecApplicationAndWaitUntilPending() {
        final JsonObject payload = getFileContentAsJson("CCApplicationStatusUpdateIT/application-created-in-criminal-courts.json",
                ImmutableMap.<String, Object>builder()
                        .put("CASE_ID", caseId)
                        .put("APP_ID", appId)
                        .put("TYPE_ID", STAT_DEC_TYPE_ID)
                        .put("APPLICATION_CODE", STAT_DEC_TYPE_CODE)
                        .put("APPLICATION_TYPE", STAT_DEC_SJP_TYPE)
                        .put("LINK_TYPE", "LINKED")
                        .put("APPLICATION_RECEIVED_DATE", DATE_RECEIVED)
                        .build());
        newPublicJmsMessageProducerClientProvider().getMessageProducerClient()
                .sendMessage(PUBLIC_COURT_APPLICATION_CREATED, payload);

        pollUntilCaseByIdIsOk(caseId, allOf(
                withJsonPath("$.ccApplicationStatus", is(ApplicationStatus.STATUTORY_DECLARATION_PENDING.name()))));
    }

    private void shareApplicationResults(final String... judicialResults) {
        final ZonedDateTime sharedTime = ZonedDateTime.now(ZoneOffset.UTC);
        final JsonObject payload = getFileContentAsJson("CCApplicationStatusUpdateIT/hearing-resulted-for-application.json",
                ImmutableMap.<String, Object>builder()
                        .put("CASE_ID", caseId)
                        .put("APP_ID", appId)
                        .put("HEARING_ID", hearingId)
                        .put("TYPE_ID", STAT_DEC_TYPE_ID)
                        .put("APPLICATION_CODE", STAT_DEC_TYPE_CODE)
                        .put("APPLICATION_TYPE", STAT_DEC_SJP_TYPE)
                        .put("LINK_TYPE", "LINKED")
                        .put("APPLICATION_RECEIVED_DATE", DATE_RECEIVED)
                        .put("SHARED_TIME", sharedTime.toString())
                        .put("HEARING_DAY", sharedTime.toLocalDate())
                        .put("JUDICIAL_RESULTS", Stream.of(judicialResults).collect(joining(",")))
                        .build());
        newPublicJmsMessageProducerClientProvider().getMessageProducerClient()
                .sendMessage(PUBLIC_EVENTS_HEARING_HEARING_RESULTED, payload);
    }

    private String judicialResult(final String resultTypeId, final String label, final String category, final boolean isAdjournmentResult) {
        return getFileContent("CCApplicationStatusUpdateIT/application-judicial-result.json",
                ImmutableMap.<String, Object>builder()
                        .put("JUDICIAL_RESULT_ID", randomUUID())
                        .put("JUDICIAL_RESULT_TYPE_ID", resultTypeId)
                        .put("HEARING_ID", hearingId)
                        .put("HEARING_DAY", LocalDate.now())
                        .put("LABEL", label)
                        .put("CATEGORY", category)
                        .put("IS_ADJOURNMENT_RESULT", isAdjournmentResult)
                        .build());
    }

    private String currentCCApplicationStatus() {
        final RequestParams request = getCaseById(caseId).build();
        final Response response = new RestClient().query(request.getUrl(), request.getMediaType(), request.getHeaders());
        final JsonObject caseDetails = JsonHelper.getJsonObject(response.readEntity(String.class));
        return caseDetails.getString("ccApplicationStatus", null);
    }

    private void createCase() {
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        stubForUserDetails(user, "ALL");

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        final CreateCase.CreateCasePayloadBuilder caseBuilder = withDefaults()
                .withId(caseId)
                .withProsecutingAuthority(prosecutingAuthority)
                .withDefendantId(defendantId)
                .withDefendantDateOfBirth(defendantDateOfBirth)
                .withOffenceId(offenceId)
                .withOffenceCode(DEFAULT_OFFENCE_CODE)
                .withUrn(urn);

        stubEnforcementAreaByPostcode(caseBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), NATIONAL_COURT_CODE, "Bedfordshire Magistrates' Court");
        stubRegionByPostcode(NATIONAL_COURT_CODE, "DEFENDANT_REGION");
        createCaseAndWaitUntilReady(caseBuilder);
    }

    private void createCaseAndWaitUntilReady(final CreateCase.CreateCasePayloadBuilder caseBuilder) {
        createCaseForPayloadBuilder(caseBuilder);
        pollUntilCaseReady(caseId);
    }

    private void assignCaseInMagistrateSession(final UUID sessionId, final UUID userId) {
        startSessionAndConfirm(sessionId, userId, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, MAGISTRATE);
        requestCaseAssignmentAndConfirm(sessionId, USER_ID, caseId);
    }


    private void dismissCase() {
        assignCaseInMagistrateSession(magistrateSessionId, user.getUserId());

        final OffenceDecision offenceDecision = DismissBuilder.withDefaults(offenceId).build();
        final DecisionCommand decision = new DecisionCommand(magistrateSessionId, caseId, "Test note", user, asList(offenceDecision), null);

        saveDecision(decision);
        pollUntilCaseStatusCompleted(caseId);
    }

}
