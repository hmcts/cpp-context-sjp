package uk.gov.moj.cpp.sjp.command.handler;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMatcher.isHandler;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.DocumentFormat.PDF;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.DELTA;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.FULL;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.TransparencyReportAggregate;
import uk.gov.moj.cpp.sjp.domain.transparency.ReportMetadata;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportGenerationFailed;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportMetadataAdded;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyPDFReportRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TransparencyReportHandlerTest {

    public static final String UPDATE_TRANSPARENCY_REPORT_DATA_COMMAND = "sjp.command.update-transparency-report-data";
    private static final String REQUEST_TRANSPARENCY_REPORT_COMMAND = "sjp.command.request-transparency-report";
    private static final String STORE_TRANSPARENCY_REPORT_DATA_COMMAND = "sjp.command.store-transparency-report-data";
    private static final String TRANSPARENCY_REPORT_GENERATION_FAILED_COMMAND = "sjp.command.transparency-report-failed";
    private static final String TEMPLATE_IDENTIFIER_ENGLISH = "PendingCasesEnglish";

    @InjectMocks
    private TransparencyReportHandler transparencyReportHandler;

    @Mock
    private Clock clock;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream transparencyReportEventStream;

    @Mock
    private TransparencyReportAggregate transparencyReportAggregate;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            TransparencyPDFReportGenerationStarted.class,
            TransparencyPDFReportRequested.class,
            TransparencyPDFReportMetadataAdded.class,
            TransparencyPDFReportGenerationFailed.class);

    @Test
    public void shouldHandleRequestTransparencyReportCommand() {
        assertThat(transparencyReportHandler, isHandler(COMMAND_HANDLER)
                .with(method("requestTransparencyReport")
                        .thatHandles(REQUEST_TRANSPARENCY_REPORT_COMMAND)
                ));
    }

    @Test
    public void shouldHandleStoreTransparencyReportDataCommand() {
        assertThat(transparencyReportHandler, isHandler(COMMAND_HANDLER)
                .with(method("storeTransparencyReportData")
                        .thatHandles(STORE_TRANSPARENCY_REPORT_DATA_COMMAND)
                ));
    }

    @Test
    public void shouldHandleTransparencyReportFailedCommand() {
        assertThat(transparencyReportHandler, isHandler(COMMAND_HANDLER)
                .with(method("transparencyReportFailed")
                        .thatHandles(TRANSPARENCY_REPORT_GENERATION_FAILED_COMMAND)
                ));
    }

    //    @Test
    public void shouldRequestTransparencyReport() throws EventStreamException {
        final JsonEnvelope requestTransparencyReportCommandJsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(REQUEST_TRANSPARENCY_REPORT_COMMAND),
                createObjectBuilder()
                        .add("format", PDF.name())
                        .add("requestType", DELTA.name())
                        .add("language", "ENGLISH")
                        .build());

        final UUID transparencyReportId = UUID.randomUUID();

        final ZonedDateTime requestedAt = ZonedDateTime.now(UTC);;
        when(clock.now()).thenReturn(requestedAt);

        final TransparencyPDFReportRequested transparencyReportRequested = new TransparencyPDFReportRequested(transparencyReportId, DELTA.name(), "ENGLISH", clock.now());
        when(eventSource.getStreamById(any(UUID.class))).thenReturn(transparencyReportEventStream);
        when(aggregateService.get(transparencyReportEventStream, TransparencyReportAggregate.class)).thenReturn(transparencyReportAggregate);
        when(transparencyReportAggregate.requestTransparencyReport(transparencyReportId, PDF.name(), DELTA.name(), "ENGLISH", clock.now())).thenReturn(Stream.of(transparencyReportRequested));

        transparencyReportHandler.requestTransparencyReport(requestTransparencyReportCommandJsonEnvelope);
        assertThat(transparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(requestTransparencyReportCommandJsonEnvelope)
                                        .withName(TransparencyPDFReportRequested.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.transparencyReportId", equalTo(transparencyReportId.toString())),
                                        withJsonPath("$.requestType", equalTo(DELTA.name())),
                                        withJsonPath("$.requestedAt", equalTo(requestedAt.toLocalDateTime() + "Z"))
                                ))))));
    }

    @Test
    public void shouldStartTransparencyReport() throws EventStreamException {
        final List<UUID> caseIds = newArrayList(randomUUID(), randomUUID(), randomUUID());
        final UUID transparencyReportId = randomUUID();

        final JsonArrayBuilder caseIdArrayBuilder = createArrayBuilder();
        caseIds.forEach(caseId -> caseIdArrayBuilder.add(caseId.toString()));


        final JsonEnvelope storeTransparencyReportDataCommandJsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(STORE_TRANSPARENCY_REPORT_DATA_COMMAND),
                createObjectBuilder()
                        .add("transparencyReportId", transparencyReportId.toString())
                        .add("caseIds", caseIdArrayBuilder));

        final TransparencyPDFReportGenerationStarted transparencyReportGenerated = new TransparencyPDFReportGenerationStarted(transparencyReportId, PDF.name(), FULL.name(), "title", "ENGLISH", caseIds);
        when(eventSource.getStreamById(transparencyReportId)).thenReturn(transparencyReportEventStream);
        when(aggregateService.get(transparencyReportEventStream, TransparencyReportAggregate.class)).thenReturn(transparencyReportAggregate);
        when(transparencyReportAggregate.startTransparencyReportGeneration(eq(caseIds), any())).thenReturn(Stream.of(transparencyReportGenerated));

        transparencyReportHandler.storeTransparencyReportData(storeTransparencyReportDataCommandJsonEnvelope);
        assertThat(transparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(storeTransparencyReportDataCommandJsonEnvelope)
                                        .withName(TransparencyPDFReportGenerationStarted.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.transparencyReportId", equalTo(transparencyReportId.toString())),
                                        withJsonPath("$.caseIds", equalTo(caseIds.stream().map(e -> e.toString()).collect(toList()))
                                        ))))

                )));
    }

    @Test
    public void shouldUpdateTransparencyReport() throws EventStreamException {
        final UUID transparencyReportId = randomUUID();
        final ReportMetadata englishReportMetadata = new ReportMetadata("transparency-report-english.pdf", 3, 1744, randomUUID());
        final JsonObject englishReportMetadataJsonObject = buildFileMetadataJsonObject(englishReportMetadata);

        final JsonEnvelope updateTransparencyReportDataCommandJsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(UPDATE_TRANSPARENCY_REPORT_DATA_COMMAND),
                createObjectBuilder()
                        .add("transparencyReportId", transparencyReportId.toString())
                        .add("metadata", englishReportMetadataJsonObject)
                        .add("language", "en")
        );

        final TransparencyPDFReportMetadataAdded metadataAdded = new TransparencyPDFReportMetadataAdded(transparencyReportId, englishReportMetadata, "en");
        when(eventSource.getStreamById(transparencyReportId)).thenReturn(transparencyReportEventStream);
        when(aggregateService.get(transparencyReportEventStream, TransparencyReportAggregate.class)).thenReturn(transparencyReportAggregate);
        when(transparencyReportAggregate.updateMetadataForLanguage(eq("en"), eq(englishReportMetadataJsonObject))).thenReturn(Stream.of(metadataAdded));

        transparencyReportHandler.updateTransparencyReportData(updateTransparencyReportDataCommandJsonEnvelope);
        assertThat(transparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(updateTransparencyReportDataCommandJsonEnvelope)
                                        .withName(TransparencyPDFReportMetadataAdded.EVENT_NAME),
                                payloadIsJson(allOf(
                                                withJsonPath("$.transparencyReportId", equalTo(transparencyReportId.toString())),
                                                withJsonPath("$.language", equalTo("en")),
                                                withJsonPath("$.metadata.fileName", equalTo("transparency-report-english.pdf")),
                                                withJsonPath("$.metadata.numberOfPages", equalTo(3)),
                                                withJsonPath("$.metadata.fileSize", equalTo(1744)),
                                                withJsonPath("$.metadata.fileId", equalTo(englishReportMetadata.getFileId().toString()))
                                        )
                                )
                        )
                )

        ));
    }

    @Test
    public void shouldRecordFailedReports() throws EventStreamException {
        final UUID transparencyReportId = randomUUID();
        final List<UUID> caseIds = asList(randomUUID(), randomUUID());

        final JsonEnvelope transparencyReportFailedEnvelope = envelopeFrom(
                metadataWithRandomUUID(TRANSPARENCY_REPORT_GENERATION_FAILED_COMMAND),
                createObjectBuilder()
                        .add("transparencyReportId", transparencyReportId.toString())
                        .add("templateIdentifier", TEMPLATE_IDENTIFIER_ENGLISH)
                        .build()
        );

        final TransparencyPDFReportGenerationFailed transparencyReportGenerationFailed = new TransparencyPDFReportGenerationFailed(transparencyReportId, TEMPLATE_IDENTIFIER_ENGLISH, caseIds, false);
        when(eventSource.getStreamById(transparencyReportId)).thenReturn(transparencyReportEventStream);
        when(aggregateService.get(transparencyReportEventStream, TransparencyReportAggregate.class)).thenReturn(transparencyReportAggregate);
        when(transparencyReportAggregate.transparencyReportFailed(TEMPLATE_IDENTIFIER_ENGLISH)).thenReturn(Stream.of(transparencyReportGenerationFailed));

        transparencyReportHandler.transparencyReportFailed(transparencyReportFailedEnvelope);
        assertThat(transparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(transparencyReportFailedEnvelope)
                                        .withName(TransparencyPDFReportGenerationFailed.EVENT_NAME),
                                payloadIsJson(allOf(
                                                withJsonPath("$.transparencyReportId", equalTo(transparencyReportId.toString())),
                                                withJsonPath("$.templateIdentifier", equalTo(TEMPLATE_IDENTIFIER_ENGLISH)),
                                                withJsonPath("$.reportGenerationPreviouslyFailed", is(false)),
                                                withJsonPath("$.caseIds[0]", equalTo(caseIds.get(0).toString())),
                                                withJsonPath("$.caseIds[1]", equalTo(caseIds.get(1).toString()))
                                        )
                                )
                        )
                )
        ));

    }

    private JsonObject buildFileMetadataJsonObject(final ReportMetadata reportMetadata) {
        return createObjectBuilder()
                .add("fileName", reportMetadata.getFileName())
                .add("numberOfPages", reportMetadata.getNumberOfPages())
                .add("fileSize", reportMetadata.getFileSize())
                .add("fileId", reportMetadata.getFileId().toString())
                .build();
    }

}