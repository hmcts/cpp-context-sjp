package uk.gov.moj.sjp.it.test;


import static com.github.tomakehurst.wiremock.client.WireMock.resetAllRequests;
import static com.google.common.collect.Sets.newHashSet;
import static java.time.LocalDate.now;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.DefendantBuilder.defaultDefendant;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.FileServiceDBHelper.createStubFile;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllIndividualProsecutorsQueries;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllReferenceData;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAnyQueryOffences;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SysDocGeneratorStub.pollSysDocGenerationRequests;
import static uk.gov.moj.sjp.it.stub.SysDocGeneratorStub.stubGenerateDocumentEndPoint;
import static uk.gov.moj.sjp.it.util.SjpDatabaseCleaner.cleanViewStore;
import static uk.gov.moj.sjp.it.util.SysDocGeneratorHelper.publishDocumentAvailablePublicEvent;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.PressTransparencyReportHelper;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.SysDocGeneratorHelper;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"squid:S1607"})
public class PressTransparencyReportIT extends BaseIntegrationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(PressTransparencyReportIT.class);

    private static final String TEMPLATE_NAME = "PressPendingCasesDeltaEnglish";
    private static final String TEMPLATE_NAME_FULL = "PressPendingCasesFullEnglish";
    private static final String SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_REQUESTED = "sjp.events.press-transparency-pdf-report-requested";
    private static final String SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_REQUESTED_JSON = "sjp.events.press-transparency-json-report-requested";
    private static final String SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_GENERATION_STARTED = "sjp.events.press-transparency-pdf-report-generation-started";
    private static final String SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_METADATA_ADDED = "sjp.events.press-transparency-pdf-report-metadata-added";
    private static final String SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_GENERATION_FAILED = "sjp.events.press-transparency-pdf-report-generation-failed";
    private static final String SJP_PUBLIC_EVENT_PRESS_TRANSPARENCY_REPORT_GENERATED = "public.sjp.press-transparency-report-generated";
    private final UUID caseId1 = randomUUID(), caseId2 = randomUUID();
    private final UUID offenceId1 = randomUUID(), offenceId2 = randomUUID();
    private PressTransparencyReportHelper pressTransparencyReportHelper = new PressTransparencyReportHelper();

    @BeforeEach
    public void beforeEachTest() throws Exception {
        resetAllRequests();
        cleanViewStore();
        stubGenerateDocumentEndPoint();
        stubAllIndividualProsecutorsQueries();
        stubAnyQueryOffences();
        stubAllReferenceData();
    }

    @AfterAll
    public static void afterAllTests() throws SQLException {
        LOGGER.info("Reinstating integration test stubs post running of {}", PressTransparencyReportIT.class.getSimpleName());
        // reinstate stubs to original state
        setup();
    }

    @Test
    public void shouldGeneratePressTransparencyPDFReports() throws IOException {

        final CreateCase.DefendantBuilder defendant1 = defaultDefendant()
                .withRandomLastName();

        final CreateCase.DefendantBuilder defendant2 = defaultDefendant()
                .withRandomLastName()
                .withDefaultShortAddress();

        stubEnforcementAreaByPostcode(defendant1.getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubEnforcementAreaByPostcode(defendant2.getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        createCase(caseId1, offenceId1, defendant1);
        createCase(caseId2, offenceId2, defendant2);

        final JsonObject payload = createObjectBuilder()
                .add("format", "PDF")
                .add("requestType", "FULL")
                .add("language", "ENGLISH")
                .build();

        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(50000)
                .subscribe(
                        SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_REQUESTED,
                        SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_GENERATION_STARTED
                )
                .run(() -> pressTransparencyReportHelper.requestToGeneratePressTransparencyReport(payload));

        final Optional<JsonEnvelope> transparencyReportRequestedEvent = eventListener.popEvent(SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_REQUESTED);
        final Optional<JsonEnvelope> transparencyReportStartedEvent = eventListener.popEvent(SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_GENERATION_STARTED);

        assertThat(transparencyReportRequestedEvent.isPresent(), is(true));
        assertThat(transparencyReportStartedEvent.isPresent(), is(true));


        final String pressTransparencyReportId = transparencyReportRequestedEvent
                .map(requestedEvent -> requestedEvent.payloadAsJsonObject().getString("pressTransparencyReportId"))
                .orElse("");

        final JsonEnvelope transparencyReportStartedEnvelope = transparencyReportStartedEvent.get();
        final JsonObject transparencyReportStartedPayload = transparencyReportStartedEnvelope.payloadAsJsonObject();

        final JsonArray caseIds = transparencyReportStartedPayload.getJsonArray("caseIds");
        final String startedTransparencyReportId = transparencyReportStartedPayload.getString("pressTransparencyReportId");
        final Set<String> savedCaseIds = newHashSet(caseId1.toString(), caseId2.toString());

        // check the cases that are created are in the transparency report generated event payload
        final List filteredCaseIDs = caseIds.getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .filter(savedCaseIds::contains)
                .collect(toList());

        assertThat(filteredCaseIDs.size(), is(2));
        assertThat(startedTransparencyReportId, is(pressTransparencyReportId));

        //verify the sys doc generation requests payloads
        final List<JSONObject> documentGenerationRequests = pollSysDocGenerationRequests(hasSize(1));
        validateDocumentGenerationRequest(documentGenerationRequests.get(0), pressTransparencyReportId);

        final UUID generatedDocumentId = createStubFile("press-transparency-report-delta-english.pdf", ZonedDateTime.now());

        final EventListener metadataAddedEventListener = new EventListener()
                .withMaxWaitTime(50000)
                .subscribe(SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_METADATA_ADDED)
                .run(() -> publishDocumentAvailablePublicEvent(
                        fromString(pressTransparencyReportId),
                        TEMPLATE_NAME_FULL,
                        generatedDocumentId)
                );

        final Optional<JsonEnvelope> metadataAdded = metadataAddedEventListener.popEvent(SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_METADATA_ADDED);
        assertThat(metadataAdded.isPresent(), is(true));

        // validate the content
        final String pressReportContent = pressTransparencyReportHelper.requestToGetTransparencyReportPressContent(generatedDocumentId.toString());
        validateThePdfContent(pressReportContent);
    }

    @Test
    public void shouldGeneratePressTransparencyJsonReports() {

        final CreateCase.DefendantBuilder defendant1 = defaultDefendant()
                .withRandomLastName();

        final CreateCase.DefendantBuilder defendant2 = defaultDefendant()
                .withRandomLastName()
                .withDefaultShortAddress();

        stubEnforcementAreaByPostcode(defendant1.getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubEnforcementAreaByPostcode(defendant2.getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        createCase(caseId1, offenceId1, defendant1);
        createCase(caseId2, offenceId2, defendant2);

        final JsonObject payload = createObjectBuilder()
                .add("format", "JSON")
                .add("requestType", "DELTA")
                .add("language", "ENGLISH")
                .build();

        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(50000)
                .subscribe(
                        SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_REQUESTED_JSON,
                        SJP_PUBLIC_EVENT_PRESS_TRANSPARENCY_REPORT_GENERATED
                )
                .run(() -> pressTransparencyReportHelper.requestToGeneratePressTransparencyReport(payload));

        final Optional<JsonEnvelope> transparencyReportRequestedEvent = eventListener.popEvent(SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_REQUESTED_JSON);
        final Optional<JsonEnvelope> transparencyReportStartedEvent = eventListener.popEvent(SJP_PUBLIC_EVENT_PRESS_TRANSPARENCY_REPORT_GENERATED);

        assertThat(transparencyReportRequestedEvent.isPresent(), is(true));
        assertThat(transparencyReportStartedEvent.isPresent(), is(true));


        final String pressTransparencyReportId = transparencyReportRequestedEvent
                .map(requestedEvent -> requestedEvent.payloadAsJsonObject().getString("pressTransparencyReportId"))
                .orElse("");

        final JsonEnvelope transparencyReportStartedEnvelope = transparencyReportStartedEvent.get();
        final JsonObject transparencyReportStartedPayload = transparencyReportStartedEnvelope.payloadAsJsonObject();

        JsonArray readyCases = transparencyReportStartedPayload.getJsonObject("listPayload").getJsonArray("readyCases");
        assertThat(readyCases.size(), is(2));
    }

    @Test
    public void shouldHandlePressTransparencyReportFailure() {

        final CreateCase.DefendantBuilder defendant1 = defaultDefendant()
                .withRandomLastName();

        final CreateCase.DefendantBuilder defendant2 = defaultDefendant()
                .withRandomLastName()
                .withDefaultShortAddress();

        stubEnforcementAreaByPostcode(defendant1.getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubEnforcementAreaByPostcode(defendant2.getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        createCase(caseId1, offenceId1, defendant1);
        createCase(caseId2, offenceId2, defendant2);

        final JsonObject payload = createObjectBuilder()
                .add("format", "PDF")
                .add("requestType", "DELTA")
                .add("language", "ENGLISH")
                .build();

        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(50000)
                .subscribe(
                        SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_REQUESTED,
                        SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_GENERATION_STARTED
                )
                .run(() -> pressTransparencyReportHelper.requestToGeneratePressTransparencyReport(payload));

        final Optional<JsonEnvelope> transparencyReportRequestedEvent = eventListener.popEvent(SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_REQUESTED);
        final Optional<JsonEnvelope> transparencyReportStartedEvent = eventListener.popEvent(SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_GENERATION_STARTED);

        assertThat(transparencyReportRequestedEvent.isPresent(), is(true));
        assertThat(transparencyReportStartedEvent.isPresent(), is(true));

        final String pressTransparencyReportId = transparencyReportRequestedEvent
                .map(requestedEvent -> requestedEvent.payloadAsJsonObject().getString("pressTransparencyReportId"))
                .orElse("");

        final EventListener generationFailedEventListener = new EventListener()
                .withMaxWaitTime(50000)
                .subscribe(SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_GENERATION_FAILED)
                .run(() -> SysDocGeneratorHelper.publishGenerationFailedPublicEvent(
                        fromString(pressTransparencyReportId),
                        TEMPLATE_NAME
                ));

        final Optional<JsonEnvelope> generationFailed = generationFailedEventListener.popEvent(SJP_EVENTS_PRESS_TRANSPARENCY_REPORT_GENERATION_FAILED);
        assertThat(generationFailed.isPresent(), is(true));
    }

    private void validateDocumentGenerationRequest(final JSONObject docGenerationRequest,
                                                   final String reportId) {
        assertThat(docGenerationRequest.getString("originatingSource"), is("sjp"));
        assertThat(docGenerationRequest.getString("templateIdentifier"), is(TEMPLATE_NAME_FULL));
        assertThat(docGenerationRequest.getString("conversionFormat"), is("pdf"));
        assertThat(docGenerationRequest.getString("sourceCorrelationId"), is(reportId));
        assertThat(docGenerationRequest.has("payloadFileServiceId"), is(true));
    }

    private CreateCasePayloadBuilder createCase(final UUID caseId,
                                                final UUID offenceId,
                                                final CreateCase.DefendantBuilder defendantBuilder) {
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

    private void validateThePdfContent(final String mockedContent) {
        assertThat(mockedContent, equalToCompressingWhiteSpace(pressTransparencyReportHelper.getStubbedContent()));
    }
}
