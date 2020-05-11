package uk.gov.moj.cpp.sjp.command.handler;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
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

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.TransparencyReportAggregate;
import uk.gov.moj.cpp.sjp.domain.transparency.ReportMetadata;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportMetadataAdded;
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TransparencyHandlerTest {

    public static final String UPDATE_TRANSPARENCY_REPORT_DATA_COMMAND = "sjp.command.update-transparency-report-data";
    private static final String REQUEST_TRANSPARENCY_REPORT_COMMAND = "sjp.command.request-transparency-report";
    private static final String STORE_TRANSPARENCY_REPORT_DATA_COMMAND = "sjp.command.store-transparency-report-data";

    @InjectMocks
    private TransparencyHandler transparencyHandler;

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
            TransparencyReportGenerationStarted.class,
            TransparencyReportRequested.class,
            TransparencyReportMetadataAdded.class);

    @Test
    public void shouldHandleRequestTransparencyReportCommand() {
        assertThat(transparencyHandler, isHandler(COMMAND_HANDLER)
                .with(method("requestTransparencyReport")
                        .thatHandles(REQUEST_TRANSPARENCY_REPORT_COMMAND)
                ));
    }

    @Test
    public void shouldHandleStoreTransparencyReportDataCommand() {
        assertThat(transparencyHandler, isHandler(COMMAND_HANDLER)
                .with(method("storeTransparencyReportData")
                        .thatHandles(STORE_TRANSPARENCY_REPORT_DATA_COMMAND)
                ));
    }

    public void shouldHandleUpdateTransparencyReportDataCommand() {
        assertThat(transparencyHandler, isHandler(COMMAND_HANDLER)
                .with(method("updateTransparencyReportData")
                        .thatHandles(UPDATE_TRANSPARENCY_REPORT_DATA_COMMAND)
                )
        );
    }

    @Test
    public void shouldRequestTransparencyReport() throws EventStreamException {
        final JsonEnvelope requestTransparencyReportCommandJsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(REQUEST_TRANSPARENCY_REPORT_COMMAND),
                createObjectBuilder());

        final UUID transparencyReportId = requestTransparencyReportCommandJsonEnvelope.metadata().id();

        final ZonedDateTime requestedAt = ZonedDateTime.now();
        when(clock.now()).thenReturn(requestedAt);

        final TransparencyReportRequested transparencyReportRequested = new TransparencyReportRequested(transparencyReportId, clock.now());
        when(eventSource.getStreamById(transparencyReportId)).thenReturn(transparencyReportEventStream);
        when(aggregateService.get(transparencyReportEventStream, TransparencyReportAggregate.class)).thenReturn(transparencyReportAggregate);
        when(transparencyReportAggregate.requestTransparencyReport(transparencyReportId, clock.now())).thenReturn(Stream.of(transparencyReportRequested));

        transparencyHandler.requestTransparencyReport(requestTransparencyReportCommandJsonEnvelope);
        assertThat(transparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(requestTransparencyReportCommandJsonEnvelope)
                                        .withName(TransparencyReportRequested.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.transparencyReportId", equalTo(transparencyReportId.toString())),
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

        final TransparencyReportGenerationStarted transparencyReportGenerated = new TransparencyReportGenerationStarted(transparencyReportId, caseIds);
        when(eventSource.getStreamById(transparencyReportId)).thenReturn(transparencyReportEventStream);
        when(aggregateService.get(transparencyReportEventStream, TransparencyReportAggregate.class)).thenReturn(transparencyReportAggregate);
        when(transparencyReportAggregate.startTransparencyReportGeneration(eq(caseIds))).thenReturn(Stream.of(transparencyReportGenerated));

        transparencyHandler.storeTransparencyReportData(storeTransparencyReportDataCommandJsonEnvelope);
        assertThat(transparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(storeTransparencyReportDataCommandJsonEnvelope)
                                        .withName(TransparencyReportGenerationStarted.EVENT_NAME),
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

        final TransparencyReportMetadataAdded metadataAdded = new TransparencyReportMetadataAdded(transparencyReportId, englishReportMetadata, "en");
        when(eventSource.getStreamById(transparencyReportId)).thenReturn(transparencyReportEventStream);
        when(aggregateService.get(transparencyReportEventStream, TransparencyReportAggregate.class)).thenReturn(transparencyReportAggregate);
        when(transparencyReportAggregate.updateMetadataForLanguage(eq("en"), eq(englishReportMetadataJsonObject))).thenReturn(Stream.of(metadataAdded));

        transparencyHandler.updateTransparencyReportData(updateTransparencyReportDataCommandJsonEnvelope);
        assertThat(transparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(updateTransparencyReportDataCommandJsonEnvelope)
                                        .withName(TransparencyReportMetadataAdded.EVENT_NAME),
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

    private JsonObject buildFileMetadataJsonObject(final ReportMetadata reportMetadata) {
        return createObjectBuilder()
                .add("fileName", reportMetadata.getFileName())
                .add("numberOfPages", reportMetadata.getNumberOfPages())
                .add("fileSize", reportMetadata.getFileSize())
                .add("fileId", reportMetadata.getFileId().toString())
                .build();
    }

}