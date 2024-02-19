package uk.gov.moj.sjp.it.test;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.Constants.PUBLIC_EVENT_SJP_PENDING_CASES_PUBLIC_LIST_GENERATED;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.DefendantBuilder.defaultDefendant;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.AssignmentHelper.requestCaseAssignment;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.DecisionHelper.verifyDecisionSaved;
import static uk.gov.moj.sjp.it.helper.FileServiceDBHelper.createStubFile;
import static uk.gov.moj.sjp.it.helper.SessionHelper.startSession;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.DVLA;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TVL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllIndividualProsecutorsQueries;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAnyQueryOffences;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubDefaultCourtByCourtHouseOUCodeQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubEndSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SchedulingStub.stubStartSjpSessionCommand;
import static uk.gov.moj.sjp.it.stub.SysDocGeneratorStub.pollSysDocGenerationRequests;
import static uk.gov.moj.sjp.it.stub.SysDocGeneratorStub.stubGenerateDocumentEndPoint;
import static uk.gov.moj.sjp.it.stub.UsersGroupsStub.stubForUserDetails;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.executeTimerJobs;
import static uk.gov.moj.sjp.it.util.ActivitiHelper.pollUntilProcessExists;
import static uk.gov.moj.sjp.it.util.CaseAssignmentRestrictionHelper.provisionCaseAssignmentRestrictions;
import static uk.gov.moj.sjp.it.util.Defaults.DEFAULT_LONDON_COURT_HOUSE_OU_CODE;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessageAsJsonObject;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.event.decision.DecisionSaved;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportGenerationFailed;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportMetadataAdded;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportRequested;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.command.CreateCase.DefendantBuilder;
import uk.gov.moj.sjp.it.helper.CaseHelper;
import uk.gov.moj.sjp.it.helper.DecisionHelper;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.TransparencyReportHelper;
import uk.gov.moj.sjp.it.model.DecisionCommand;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.jms.MessageConsumer;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.google.common.collect.Sets;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore("Temporarily disabled until fixed ATCM-6621")
public class TransparencyReportIT extends BaseIntegrationTest {

    private static final String DOCUMENT_GENERATION_FAILED_EVENT_NAME = "public.systemdocgenerator.events.generation-failed";
    private static final String DOCUMENT_AVAILABLE_EVENT_NAME = "public.systemdocgenerator.events.document-available";

    private TransparencyReportHelper transparencyReportHelper = new TransparencyReportHelper();
    private final UUID caseId1 = randomUUID(), caseId2 = randomUUID();
    private final UUID offenceId1 = randomUUID(), offenceId2 = randomUUID();

    private static final String SJP_EVENTS_TRANSPARENCY_REPORT_REQUESTED = "sjp.events.transparency-report-requested";
    private static final String SJP_EVENTS_TRANSPARENCY_REPORT_GENERATION_STARTED = "sjp.events.transparency-report-generation-started";

    private final MessageConsumer publicMessageConsumer = TopicUtil.publicEvents.createConsumerForMultipleSelectors(PUBLIC_EVENT_SJP_PENDING_CASES_PUBLIC_LIST_GENERATED);

    @Before
    public void setUp() throws Exception {
        new SjpDatabaseCleaner().cleanViewStore();
        stubAllIndividualProsecutorsQueries();
        stubAnyQueryOffences();
        stubGenerateDocumentEndPoint();
    }

    @Test
    public void shouldGenerateTransparencyReports() throws IOException {

        final DefendantBuilder defendant1 = defaultDefendant()
                .withRandomLastName();

        final DefendantBuilder defendant2 = defaultDefendant()
                .withRandomLastName()
                .withDefaultShortAddress();

        stubEnforcementAreaByPostcode(defendant1.getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        createCase(caseId1, offenceId1, defendant1);
        createCase(caseId2, offenceId2, defendant2);

        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(100000)
                .subscribe(TransparencyReportRequested.EVENT_NAME)
                .subscribe(TransparencyReportGenerationStarted.EVENT_NAME)
                .run(transparencyReportHelper::requestToGenerateTransparencyReport);

        final Optional<JsonEnvelope> transparencyReportRequestedEvent = eventListener.popEvent(TransparencyReportRequested.EVENT_NAME);
        final Optional<JsonEnvelope> transparencyReportGenerationStarted = eventListener.popEvent(TransparencyReportGenerationStarted.EVENT_NAME);

        assertThat(transparencyReportRequestedEvent.isPresent(), is(true));
        assertThat(transparencyReportGenerationStarted.isPresent(), is(true));

        final String transparencyReportId = transparencyReportRequestedEvent
                .map(requestedEvent -> requestedEvent.payloadAsJsonObject().getString("transparencyReportId"))
                .orElse("");

        final JsonEnvelope transparencyReportStartedEnvelope = transparencyReportGenerationStarted.get();

        final JsonObject transparencyReportStartedPayload = transparencyReportStartedEnvelope.payloadAsJsonObject();
        final JsonArray caseIds = transparencyReportStartedPayload.getJsonArray("caseIds");
        final String startedTransparencyReportId = transparencyReportStartedPayload.getString("transparencyReportId");
        final Set<String> savedCaseIds = Sets.newHashSet(caseId1.toString(), caseId2.toString());

        // check the cases that are created are in the transparency report generated event payload
        final List filteredCaseIDs = caseIds.getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .filter(savedCaseIds::contains)
                .collect(toList());

        assertThat(filteredCaseIDs.size(), is(2));
        assertThat(startedTransparencyReportId, is(transparencyReportId));

        //verify the sys doc generation requests payloads
        final List<JSONObject> documentGenerationRequests = pollSysDocGenerationRequests(hasSize(2));
        // validate the english and welsh payloads
        validateDocumentGenerationRequest(documentGenerationRequests.get(0), "PendingCasesEnglish", transparencyReportId);
        validateDocumentGenerationRequest(documentGenerationRequests.get(1), "PendingCasesWelsh", transparencyReportId);


        final UUID generatedDocumentEnglishId = createStubFile("transparency-report-english.pdf", ZonedDateTime.now());
        final UUID generatedDocumentWelshId = createStubFile("transparency-report-welsh.pdf", ZonedDateTime.now());


        final EventListener metadataAddedEventListener = new EventListener()
                .withMaxWaitTime(50000)
                .subscribe(TransparencyReportMetadataAdded.EVENT_NAME)
                .run(() -> publishSysDocPublicEvents(transparencyReportId, generatedDocumentEnglishId, generatedDocumentWelshId));

        final Optional<JsonEnvelope> englishMetadataAdded = metadataAddedEventListener.popEvent(TransparencyReportMetadataAdded.EVENT_NAME);

        assertThat(englishMetadataAdded.isPresent(), is(true));


        // get the report metadata
        final Matcher matcher = withJsonPath("reportsMetadata.*", allOf(
                hasItem(
                        isJson(
                                withJsonPath("fileId", equalTo(generatedDocumentEnglishId.toString()))
                        )),
                hasItem(
                        isJson(
                                withJsonPath("fileId", equalTo(generatedDocumentWelshId.toString()))
                        ))
        ));

        final JsonObject reportsMetadata = transparencyReportHelper.pollForTransparencyReportMetadata(matcher);
        final JsonArray reportsArray = reportsMetadata.getJsonArray("reportsMetadata");
        final JsonObject englishReport = reportsArray.getJsonObject(0);
        final JsonObject welshReport = reportsArray.getJsonObject(1);

        validateMetadata(englishReport, generatedDocumentEnglishId.toString(), false);
        validateMetadata(welshReport, generatedDocumentWelshId.toString(), true);

        // validate the content
        final String englishContent = transparencyReportHelper.requestToGetTransparencyReportContent(generatedDocumentEnglishId.toString());
        validateThePdfContent(englishContent);

        final String welshContent = transparencyReportHelper.requestToGetTransparencyReportContent(generatedDocumentWelshId.toString());
        validateThePdfContent(welshContent);

        final JsonEnvelope sjpCaseListEnglish = getEventFromTopic(publicMessageConsumer);
        verifySJPPendingPublicListEvent(sjpCaseListEnglish, "ENGLISH",2);
        final JsonEnvelope sjpCaseListWelsh = getEventFromTopic(publicMessageConsumer);
        verifySJPPendingPublicListEvent(sjpCaseListWelsh, "WELSH", 2);

    }

    private void verifySJPPendingPublicListEvent(final JsonEnvelope event, final String language, final int numberOfCases) {
        assertThat(event, notNullValue());
        final JsonObject payload = event.payloadAsJsonObject();
        assertThat(payload, notNullValue());
        assertThat(language, is(payload.getString("language")));
        final JsonObject listPayload = payload.getJsonObject("listPayload");
        assertThat(numberOfCases, is(listPayload.getInt("totalNumberOfRecords")));
    }

    private JsonEnvelope getEventFromTopic(final MessageConsumer messageConsumer) {
        return retrieveMessageAsJsonObject(messageConsumer)
                .map(event -> new DefaultJsonObjectEnvelopeConverter().asEnvelope(event)).orElse(null);
    }

    @Test
    public void shouldRollbackErrorCountWhenReportGenerationHasFailed() throws IOException {
        // Creates cases to be shown in the report
        final CreateCase.DefendantBuilder defendant1 = defaultDefendant().withRandomLastName();
        final CreateCase.DefendantBuilder defendant2 = defaultDefendant().withRandomLastName().withDefaultShortAddress();
        stubEnforcementAreaByPostcode(defendant1.getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");
        createCase(caseId1, offenceId1, defendant1);
        createCase(caseId2, offenceId2, defendant2);

        // Request report generation
        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(50000)
                .subscribe(TransparencyReportRequested.EVENT_NAME)
                .subscribe(TransparencyReportGenerationStarted.EVENT_NAME)
                .run(transparencyReportHelper::requestToGenerateTransparencyReport);
        final Optional<JsonEnvelope> transparencyReportRequestedEvent = eventListener.popEvent(TransparencyReportRequested.EVENT_NAME);
        final Optional<JsonEnvelope> transparencyReportGenerationStarted = eventListener.popEvent(TransparencyReportGenerationStarted.EVENT_NAME);
        assertThat(transparencyReportRequestedEvent.isPresent(), is(true));
//        assertThat(transparencyReportGenerationStarted.isPresent(), is(true));
        final String transparencyReportId = transparencyReportRequestedEvent
                .map(requestedEvent -> requestedEvent.payloadAsJsonObject().getString("transparencyReportId"))
                .orElse("");


        final JsonEnvelope transparencyReportStartedEnvelope = transparencyReportGenerationStarted.get();
        final JsonObject transparencyReportStartedPayload = transparencyReportStartedEnvelope.payloadAsJsonObject();

        final JsonArray caseIds = transparencyReportStartedPayload.getJsonArray("caseIds");
        final String startedTransparencyReportId = transparencyReportStartedPayload.getString("transparencyReportId");
        final Set<String> savedCaseIds = Sets.newHashSet(caseId1.toString(), caseId2.toString());

        // check the cases that are created are in the transparency report generated event payload
        final List filteredCaseIDs = caseIds.getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .filter(savedCaseIds::contains)
                .collect(toList());

        assertThat(filteredCaseIDs.size(), is(2));
        assertThat(startedTransparencyReportId, is(transparencyReportId));

        final EventListener metadataAddedEventListener = new EventListener()
                .withMaxWaitTime(50000)
                .subscribe(TransparencyReportMetadataAdded.EVENT_NAME)
                .subscribe(TransparencyReportGenerationFailed.EVENT_NAME)
                .subscribe(DOCUMENT_GENERATION_FAILED_EVENT_NAME)
                .run(() -> publishSysDocPublicFailedEvents(transparencyReportId, randomUUID(), randomUUID()));

        final Optional<JsonEnvelope> transparencyReportFailed = metadataAddedEventListener.popEvent(TransparencyReportGenerationFailed.EVENT_NAME);
        assertThat(transparencyReportFailed.isPresent(), is(true));
        transparencyReportFailed.ifPresent(transparencyReportFailedEvent -> {
            final JsonObject failedEventPayload = transparencyReportFailedEvent.payloadAsJsonObject();
            final JsonArray failedCaseIds = failedEventPayload.getJsonArray("caseIds");
            final Set<String> failedCaseIdsSet = failedCaseIds.getValuesAs(JsonString.class)
                    .stream()
                    .map(JsonString::getString)
                    .collect(Collectors.toSet());
            assertThat(savedCaseIds, equalTo(failedCaseIdsSet));
            assertThat(failedEventPayload.getString("templateIdentifier"), equalTo("PendingCasesEnglish"));
            assertThat(failedEventPayload.getBoolean("reportGenerationPreviouslyFailed"), equalTo(false));
        });

    }

    private static JsonObject startSessionAndRequestAssignment(final UUID sessionId, final SessionType sessionType) {
        final JsonEnvelope session = startSession(sessionId, USER_ID, DEFAULT_LONDON_COURT_HOUSE_OU_CODE, sessionType).get();
        requestCaseAssignment(sessionId, USER_ID);
        return session.payloadAsJsonObject();
    }

    @Test
    public void shouldDisplayCaseWhenReportingRestrictionIsRevoked() {
        final UUID offenceId2 = randomUUID();
        final UUID offenceId3 = randomUUID();
        final User user = new User("John", "Smith", USER_ID);
        final UUID sessionId = randomUUID();

        final DefendantBuilder defendant = defaultDefendant().withRandomLastName().withDateOfBirth(LocalDate.of(2000, 9, 18));

        stubEnforcementAreaByPostcode(defendant.getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");
        stubStartSjpSessionCommand();
        stubEndSjpSessionCommand();
        stubDefaultCourtByCourtHouseOUCodeQuery();
        stubForUserDetails(user, "ALL");

        provisionCaseAssignmentRestrictions(Sets.newHashSet(TFL, TVL, DVLA));

        createCase(caseId1, defendant, offenceId1, offenceId2, offenceId3);

        final LocalDate adjournTo = now().plusDays(10);
        final Withdraw withdraw = new Withdraw(null, createOffenceDecisionInformation(offenceId1, NO_VERDICT), randomUUID());
        final Adjourn adjournDecision = new Adjourn(null,
                asList(
                        createOffenceDecisionInformation(offenceId2, NO_VERDICT),
                        createOffenceDecisionInformation(offenceId3, NO_VERDICT)
                ),
                "adjourn reason",
                adjournTo, PressRestriction.requested("Session 1"));

        startSessionAndRequestAssignment(sessionId, MAGISTRATE);
        final List<OffenceDecision> offencesDecisions = asList(withdraw, adjournDecision);
        final DecisionCommand decisionCommand = new DecisionCommand(sessionId, caseId1, null, user, offencesDecisions, null);

        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(50000)
                .subscribe(DecisionSaved.EVENT_NAME)
                .run(() -> DecisionHelper.saveDecision(decisionCommand));

        final DecisionSaved decisionSaved = eventListener.popEventPayload(DecisionSaved.class);
        verifyDecisionSaved(decisionCommand, decisionSaved);

        final String pendingAdjournmentProcess = pollUntilProcessExists("timerTimeout", caseId1.toString());
        executeTimerJobs(pendingAdjournmentProcess);

        CaseHelper.pollUntilCaseReady(caseId1);

        final UUID sessionId2 = randomUUID();
        startSessionAndRequestAssignment(sessionId2, MAGISTRATE);

        final Adjourn adjournDecision2 = new Adjourn(null,
                asList(
                        createOffenceDecisionInformation(offenceId2, NO_VERDICT),
                        createOffenceDecisionInformation(offenceId3, NO_VERDICT)
                ),
                "adjourn reason",
                adjournTo, PressRestriction.revoked());

        final List<OffenceDecision> offencesDecisions2 = asList(adjournDecision2);
        final DecisionCommand decisionCommand2 = new DecisionCommand(sessionId2, caseId1, null, user, offencesDecisions2, null);

        eventListener.run(() -> DecisionHelper.saveDecision(decisionCommand2));

        final DecisionSaved decisionSaved2 = eventListener.popEventPayload(DecisionSaved.class);
        verifyDecisionSaved(decisionCommand2, decisionSaved2);

        final String pendingAdjournmentProcess2 = pollUntilProcessExists("timerTimeout", caseId1.toString());
        executeTimerJobs(pendingAdjournmentProcess2);

        CaseHelper.pollUntilCaseReady(caseId1);

        eventListener.subscribe(SJP_EVENTS_TRANSPARENCY_REPORT_REQUESTED, SJP_EVENTS_TRANSPARENCY_REPORT_GENERATION_STARTED)
                .run(transparencyReportHelper::requestToGenerateTransparencyReport);

        final Optional<JsonEnvelope> transparencyReportRequestedEvent = eventListener.popEvent(SJP_EVENTS_TRANSPARENCY_REPORT_REQUESTED);
        final Optional<JsonEnvelope> transparencyReportGenerationStarted = eventListener.popEvent(SJP_EVENTS_TRANSPARENCY_REPORT_GENERATION_STARTED);

        assertThat(transparencyReportRequestedEvent.isPresent(), is(true));
        assertThat(transparencyReportGenerationStarted.isPresent(), is(true));

        final JsonEnvelope transparencyReportStartedEnvelope = transparencyReportGenerationStarted.get();

        final JsonObject transparencyReportStartedPayload = transparencyReportStartedEnvelope.payloadAsJsonObject();
        final JsonArray caseIds = transparencyReportStartedPayload.getJsonArray("caseIds");
        final Set<String> expectedCaseIds = Sets.newHashSet(caseId1.toString());

        // check the cases that are created are in the transparency report generated event payload
        final List filteredCaseIDs = caseIds.getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .filter(expectedCaseIds::contains)
                .collect(toList());

        assertThat(filteredCaseIDs.isEmpty(), is(false));

    }

    private void publishSysDocPublicEvents(final String transparencyReportId, final UUID generatedDocumentEnglishId, final UUID generatedDocumentWelshId) {
        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage(DOCUMENT_AVAILABLE_EVENT_NAME,
                    documentAvailablePayload(randomUUID(), "PendingCasesEnglish", transparencyReportId, generatedDocumentEnglishId));
            producerClient.sendMessage(DOCUMENT_AVAILABLE_EVENT_NAME,
                    documentAvailablePayload(randomUUID(), "PendingCasesWelsh", transparencyReportId, generatedDocumentWelshId));
        }
    }

    private void publishSysDocPublicFailedEvents(final String transparencyReportId, final UUID generatedDocumentEnglishId, final UUID generatedDocumentWelshId) {
        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage(DOCUMENT_GENERATION_FAILED_EVENT_NAME,
                    generationFailedPayload(randomUUID(), "PendingCasesEnglish", transparencyReportId));
            producerClient.sendMessage(DOCUMENT_GENERATION_FAILED_EVENT_NAME,
                    generationFailedPayload(randomUUID(), "PendingCasesWelsh", transparencyReportId));
        }
    }

    private JsonObject documentAvailablePayload(final UUID templatePayloadId, final String templateIdentifier, final String reportId, final UUID generatedDocumentId) {
        return createObjectBuilder()
                .add("payloadFileServiceId", templatePayloadId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", "sjp")
                .add("documentFileServiceId", generatedDocumentId.toString())
                .add("generatedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("generateVersion", 1)
                .build();
    }

    private JsonObject generationFailedPayload(final UUID templatePayloadId, final String templateIdentifier, final String reportId) {
        return createObjectBuilder()
                .add("payloadFileServiceId", templatePayloadId.toString())
                .add("templateIdentifier", templateIdentifier)
                .add("conversionFormat", "pdf")
                .add("requestedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("failedTime", ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT))
                .add("sourceCorrelationId", reportId)
                .add("originatingSource", "sjp")
                .add("reason", "The file was too large")
                .build();
    }

    private void validateMetadata(final JsonObject reportObject,
                                  final String generatedFileId,
                                  final boolean welsh) {
        assertThat(reportObject.getString("fileId"), is(generatedFileId));
        assertThat(reportObject.getString("reportIn"), is(welsh ? "Welsh" : "English"));
        assertThat(reportObject.get("size").toString(), is(notNullValue()));
        assertThat(reportObject.getInt("pages"), is(2));
        assertThat(reportObject.getString("generatedAt"), is(notNullValue()));
    }

    private void validateDocumentGenerationRequest(final JSONObject docGenerationRequest,
                                                   final String templateName,
                                                   final String transparencyReportId) {
        assertThat(docGenerationRequest.getString("originatingSource"), is("sjp"));
        assertThat(docGenerationRequest.getString("templateIdentifier"), is(templateName));
        assertThat(docGenerationRequest.getString("conversionFormat"), is("pdf"));
        assertThat(docGenerationRequest.getString("sourceCorrelationId"), is(transparencyReportId));
        assertThat(docGenerationRequest.has("payloadFileServiceId"), is(true));
    }


    private CreateCasePayloadBuilder createCase(final UUID caseId,
                                                final UUID offenceId,
                                                final DefendantBuilder defendantBuilder) {
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);
        final CreateCasePayloadBuilder createCasePayloadBuilder = CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withOffenceId(offenceId)
                .withPostingDate(postingDate)
                .withDefendantBuilder(defendantBuilder);

        createCaseForPayloadBuilder(createCasePayloadBuilder);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        pollUntilCaseReady(createCasePayloadBuilder.getId());
        return createCasePayloadBuilder;
    }

    private CreateCasePayloadBuilder createCase(final UUID caseId,
                                                final DefendantBuilder defendantBuilder,
                                                final UUID offenceId1,
                                                final UUID offenceId2,
                                                final UUID offenceId3) {
        final LocalDate postingDate = now().minusDays(NOTICE_PERIOD_IN_DAYS + 1);

        final CreateCase.OffenceBuilder offence1 = CreateCase.OffenceBuilder.withDefaults()
                .withId(offenceId1).withPressRestrictable(true).withOffenceCommittedDate(LocalDate.of(2019, 8, 11));
        final CreateCase.OffenceBuilder offence2 = CreateCase.OffenceBuilder.withDefaults()
                .withId(offenceId2).withPressRestrictable(true).withOffenceCommittedDate(LocalDate.of(2019, 8, 11));
        final CreateCase.OffenceBuilder offence3 = CreateCase.OffenceBuilder.withDefaults()
                .withId(offenceId3).withPressRestrictable(false).withOffenceCommittedDate(LocalDate.of(2019, 8, 11));

        final CreateCasePayloadBuilder createCasePayloadBuilder = CreateCasePayloadBuilder
                .withDefaults()
                .withId(caseId)
                .withPostingDate(postingDate)
                .withDefendantBuilder(defendantBuilder)
                .withOffenceBuilders(offence1, offence2, offence3);


        createCaseForPayloadBuilder(createCasePayloadBuilder);

        final ProsecutingAuthority prosecutingAuthority = createCasePayloadBuilder.getProsecutingAuthority();
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());

        pollUntilCaseReady(createCasePayloadBuilder.getId());
        return createCasePayloadBuilder;
    }

    private void validateThePdfContent(final String mockedContent) {
        assertThat(mockedContent, equalToCompressingWhiteSpace(transparencyReportHelper.getStubbedContent()));
    }
}
