package uk.gov.moj.cpp.sjp.command.handler;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
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
import uk.gov.moj.cpp.sjp.event.transparency.TransparencyReportGenerated;
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

    private static final String REQUEST_TRANSPARENCY_REPORT_COMMAND = "sjp.command.request-transparency-report";
    private static final String STORE_TRANSPARENCY_REPORT_DATA_COMMAND = "sjp.command.store-transparency-report-data";

    public static final UUID TRANSPARENCY_REPORT_STREAM_ID_IN_TEST = fromString("37c62719-f1cc-4a84-bca4-14087d9d826c");

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
            TransparencyReportGenerated.class, TransparencyReportRequested.class);

    @Test
    public void shouldHandleRequestTransparencyReportCommand() {
        assertThat(transparencyHandler, isHandler(COMMAND_HANDLER)
                .with(method("requestTransparencyReport")
                        .thatHandles("sjp.command.request-transparency-report")
                ));
    }

    @Test
    public void shouldHandleStoreTransparencyReportDataCommand() {
        assertThat(transparencyHandler, isHandler(COMMAND_HANDLER)
                .with(method("storeTransparencyReportData")
                        .thatHandles("sjp.command.store-transparency-report-data")
                ));
    }

    @Test
    public void shouldRequestTransparencyReport() throws EventStreamException {
        final JsonEnvelope requestTransparencyReportCommandJsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(REQUEST_TRANSPARENCY_REPORT_COMMAND),
                createObjectBuilder());

        final ZonedDateTime requestedAt = ZonedDateTime.now();
        when(clock.now()).thenReturn(requestedAt);

        final TransparencyReportRequested transparencyReportRequested = new TransparencyReportRequested(clock.now());
        when(eventSource.getStreamById(TRANSPARENCY_REPORT_STREAM_ID_IN_TEST)).thenReturn(transparencyReportEventStream);
        when(aggregateService.get(transparencyReportEventStream, TransparencyReportAggregate.class)).thenReturn(transparencyReportAggregate);
        when(transparencyReportAggregate.requestTransparencyReport(clock.now())).thenReturn(Stream.of(transparencyReportRequested));

        transparencyHandler.requestTransparencyReport(requestTransparencyReportCommandJsonEnvelope);
        assertThat(transparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(requestTransparencyReportCommandJsonEnvelope)
                                        .withName(TransparencyReportRequested.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.requestedAt", equalTo(requestedAt.toLocalDateTime() + "Z"))
                                ))))));
    }

    @Test
    public void shouldGenerateTransparencyReport() throws EventStreamException {
        final List<UUID> caseIds = newArrayList(randomUUID(), randomUUID(), randomUUID());
        final ReportMetadata englishReportMetadata = new ReportMetadata("transparency-report-english.pdf", 3, 1744, randomUUID());
        final ReportMetadata welshReportMetadata = new ReportMetadata("transparency-report-welsh.pdf", 2, 1235, randomUUID());

        final JsonObject englishReportMetadataJsonObject = buildFileMetadataJsonObject(englishReportMetadata);
        final JsonObject welshReportMetadataJsonObject = buildFileMetadataJsonObject(welshReportMetadata);

        final JsonArrayBuilder caseIdArrayBuilder = createArrayBuilder();
        caseIds.forEach(caseId -> caseIdArrayBuilder.add(caseId.toString()));


        final JsonEnvelope storeTransparencyReportDataCommandJsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(STORE_TRANSPARENCY_REPORT_DATA_COMMAND),
                createObjectBuilder()
                        .add("caseIds", caseIdArrayBuilder)
                        .add("englishReportMetadata", englishReportMetadataJsonObject)
                        .add("welshReportMetadata", welshReportMetadataJsonObject));

        final TransparencyReportGenerated transparencyReportGenerated = new TransparencyReportGenerated(caseIds, englishReportMetadata, welshReportMetadata);
        when(eventSource.getStreamById(TRANSPARENCY_REPORT_STREAM_ID_IN_TEST)).thenReturn(transparencyReportEventStream);
        when(aggregateService.get(transparencyReportEventStream, TransparencyReportAggregate.class)).thenReturn(transparencyReportAggregate);
        when(transparencyReportAggregate.generateTransparencyReport(eq(caseIds), eq(englishReportMetadataJsonObject), eq(welshReportMetadataJsonObject))).thenReturn(Stream.of(transparencyReportGenerated));

        transparencyHandler.storeTransparencyReportData(storeTransparencyReportDataCommandJsonEnvelope);
        assertThat(transparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(storeTransparencyReportDataCommandJsonEnvelope)
                                        .withName(TransparencyReportGenerated.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.englishReportMetadata.fileName", equalTo(englishReportMetadata.getFileName())),
                                        withJsonPath("$.englishReportMetadata.numberOfPages", equalTo(englishReportMetadata.getNumberOfPages())),
                                        withJsonPath("$.englishReportMetadata.fileSize", equalTo(englishReportMetadata.getFileSize())),
                                        withJsonPath("$.englishReportMetadata.fileId", equalTo(englishReportMetadata.getFileId().toString())), withJsonPath("$.englishReportMetadata.fileName", equalTo(englishReportMetadata.getFileName())),
                                        withJsonPath("$.welshReportMetadata.numberOfPages", equalTo(welshReportMetadata.getNumberOfPages())),
                                        withJsonPath("$.welshReportMetadata.fileSize", equalTo(welshReportMetadata.getFileSize())),
                                        withJsonPath("$.welshReportMetadata.fileId", equalTo(welshReportMetadata.getFileId().toString())),
                                        withJsonPath("$.caseIds", equalTo(caseIds.stream().map(e -> e.toString()).collect(toList()))
                                        ))))

                )));
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