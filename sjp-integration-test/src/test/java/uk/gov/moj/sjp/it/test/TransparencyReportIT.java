package uk.gov.moj.sjp.it.test;


import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
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
import static uk.gov.moj.sjp.it.Constants.NOTICE_PERIOD_IN_DAYS;
import static uk.gov.moj.sjp.it.command.CreateCase.CreateCasePayloadBuilder;
import static uk.gov.moj.sjp.it.command.CreateCase.DefendantBuilder.defaultDefendant;
import static uk.gov.moj.sjp.it.command.CreateCase.createCaseForPayloadBuilder;
import static uk.gov.moj.sjp.it.helper.CaseHelper.pollUntilCaseReady;
import static uk.gov.moj.sjp.it.helper.FileServiceDBHelper.createStubFile;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAllIndividualProsecutorsQueries;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubAnyQueryOffences;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;
import static uk.gov.moj.sjp.it.stub.SysDocGeneratorStub.pollSysDocGenerationRequests;
import static uk.gov.moj.sjp.it.stub.SysDocGeneratorStub.stubDocGeneratorEndPoint;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.messaging.MessageProducerClient;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.EventListener;
import uk.gov.moj.sjp.it.helper.FileServiceDBHelper;
import uk.gov.moj.sjp.it.helper.TransparencyReportHelper;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.SjpDatabaseCleaner;

import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

import com.google.common.collect.Sets;
import org.hamcrest.Matcher;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class TransparencyReportIT extends BaseIntegrationTest {

    private TransparencyReportHelper transparencyReportHelper = new TransparencyReportHelper();
    private final UUID caseId1 = randomUUID(), caseId2 = randomUUID();
    private final UUID offenceId1 = randomUUID(), offenceId2 = randomUUID();

    private static final String SJP_EVENTS_TRANSPARENCY_REPORT_REQUESTED = "sjp.events.transparency-report-requested";
    private static final String SJP_EVENTS_TRANSPARENCY_REPORT_GENERATION_STARTED = "sjp.events.transparency-report-generation-started";
    private static final String SJP_EVENTS_TRANSPARENCY_REPORT_METADATA_ADDED = "sjp.events.transparency-report-metadata-added";

    @Before
    public void setUp() throws Exception {
        new SjpDatabaseCleaner().cleanViewStore();
        stubAllIndividualProsecutorsQueries();
        stubAnyQueryOffences();
        stubDocGeneratorEndPoint();
    }

    @Test
    public void shouldGenerateTransparencyReports() throws IOException {

        final CreateCase.DefendantBuilder defendant1 = defaultDefendant()
                .withRandomLastName();

        final CreateCase.DefendantBuilder defendant2 = defaultDefendant()
                .withRandomLastName()
                .withDefaultShortAddress();

        stubEnforcementAreaByPostcode(defendant1.getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");

        createCase(caseId1, offenceId1, defendant1);
        createCase(caseId2, offenceId2, defendant2);

        final EventListener eventListener = new EventListener()
                .withMaxWaitTime(50000)
                .subscribe(SJP_EVENTS_TRANSPARENCY_REPORT_REQUESTED, SJP_EVENTS_TRANSPARENCY_REPORT_GENERATION_STARTED)
                .run(transparencyReportHelper::requestToGenerateTransparencyReport);

        final Optional<JsonEnvelope> transparencyReportRequestedEvent = eventListener.popEvent(SJP_EVENTS_TRANSPARENCY_REPORT_REQUESTED);
        final Optional<JsonEnvelope> transparencyReportGenerationStarted = eventListener.popEvent(SJP_EVENTS_TRANSPARENCY_REPORT_GENERATION_STARTED);

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
                .subscribe(SJP_EVENTS_TRANSPARENCY_REPORT_METADATA_ADDED)
                .run(() -> publishSysDocPublicEvents(transparencyReportId, generatedDocumentEnglishId, generatedDocumentWelshId));

        final Optional<JsonEnvelope> englishMetadataAdded = metadataAddedEventListener.popEvent(SJP_EVENTS_TRANSPARENCY_REPORT_METADATA_ADDED);

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
    }

    private void publishSysDocPublicEvents(final String transparencyReportId, final UUID generatedDocumentEnglishId, final UUID generatedDocumentWelshId) {
        try (final MessageProducerClient producerClient = new MessageProducerClient()) {
            producerClient.startProducer("public.event");
            producerClient.sendMessage("public.systemdocgenerator.events.document-available",
                    documentAvailablePayload(randomUUID(), "PendingCasesEnglish", transparencyReportId, generatedDocumentEnglishId));
            producerClient.sendMessage("public.systemdocgenerator.events.document-available",
                    documentAvailablePayload(randomUUID(), "PendingCasesWelsh", transparencyReportId, generatedDocumentWelshId));
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
        assertThat(mockedContent, equalToCompressingWhiteSpace(transparencyReportHelper.getStubbedContent()));
    }
}
