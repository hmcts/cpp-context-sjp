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

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.PressTransparencyReportAggregate;
import uk.gov.moj.cpp.sjp.domain.transparency.ReportMetadata;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyReportGenerationStarted;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyReportMetadataAdded;
import uk.gov.moj.cpp.sjp.event.transparency.PressTransparencyReportRequested;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PressTransparencyReportHandlerTest {

    private static final String REQUEST_PRESS_TRANSPARENCY_REPORT_COMMAND = "sjp.command.request-press-transparency-report";
    private static final String STORE_PRESS_TRANSPARENCY_REPORT_DATA_COMMAND = "sjp.command.store-press-transparency-report-data";
    private static final String UPDATE_PRESS_TRANSPARENCY_REPORT_DATA_COMMAND = "sjp.command.update-press-transparency-report-data";

    @InjectMocks
    private PressTransparencyReportHandler pressTransparencyReportHandler;

    @Mock
    private Clock clock;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream pressTransparencyReportEventStream;

    @Mock
    private PressTransparencyReportAggregate pressTransparencyReportAggregate;

    @Spy
    private final ObjectMapper mapper = new ObjectMapperProducer().objectMapper();

    @Spy
    @InjectMocks
    private JsonObjectToObjectConverter converter;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            PressTransparencyReportRequested.class,
            PressTransparencyReportGenerationStarted.class,
            PressTransparencyReportMetadataAdded.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldHandleRequestTransparencyReportCommand() {
        assertThat(pressTransparencyReportHandler, isHandler(COMMAND_HANDLER)
                .with(method("requestPressTransparencyReport")
                        .thatHandles(REQUEST_PRESS_TRANSPARENCY_REPORT_COMMAND)
                ));
    }

    @Test
    public void shouldRequestTransparencyReport() throws EventStreamException {
        final JsonEnvelope requestPressTransparencyReportCommandJsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(REQUEST_PRESS_TRANSPARENCY_REPORT_COMMAND),
                createObjectBuilder());

        final ZonedDateTime requestedAt = ZonedDateTime.now();
        final UUID pressTransparencyReportId = requestPressTransparencyReportCommandJsonEnvelope.metadata().id();
        when(clock.now()).thenReturn(requestedAt);

        final PressTransparencyReportRequested pressTransparencyReportRequested = new PressTransparencyReportRequested(pressTransparencyReportId, clock.now());
        when(eventSource.getStreamById(pressTransparencyReportId)).thenReturn(pressTransparencyReportEventStream);
        when(aggregateService.get(pressTransparencyReportEventStream, PressTransparencyReportAggregate.class)).thenReturn(pressTransparencyReportAggregate);
        when(pressTransparencyReportAggregate.requestPressTransparencyReport(pressTransparencyReportId, clock.now())).thenReturn(Stream.of(pressTransparencyReportRequested));

        pressTransparencyReportHandler.requestPressTransparencyReport(requestPressTransparencyReportCommandJsonEnvelope);
        assertThat(pressTransparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(requestPressTransparencyReportCommandJsonEnvelope)
                                        .withName(PressTransparencyReportRequested.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.pressTransparencyReportId", equalTo(pressTransparencyReportId.toString())),
                                        withJsonPath("$.requestedAt", equalTo(requestedAt.toLocalDateTime() + "Z"))
                                ))))));
    }

    @Test
    public void shouldHandleStorePressTransparencyReportDataCommand() {
        assertThat(pressTransparencyReportHandler, isHandler(COMMAND_HANDLER)
                .with(method("storePressTransparencyReportMetadata")
                        .thatHandles(STORE_PRESS_TRANSPARENCY_REPORT_DATA_COMMAND)
                ));
    }

    @Test
    public void shouldStartReportGeneration() throws EventStreamException {
        final List<UUID> caseIds = newArrayList(randomUUID(), randomUUID(), randomUUID());
        final UUID reportId = randomUUID();

        final JsonArrayBuilder caseIdArrayBuilder = createArrayBuilder();
        caseIds.forEach(caseId -> caseIdArrayBuilder.add(caseId.toString()));


        final JsonEnvelope storePressTransparencyReportDataCommandJsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(STORE_PRESS_TRANSPARENCY_REPORT_DATA_COMMAND),
                createObjectBuilder()
                        .add("caseIds", caseIdArrayBuilder)
                        .add("pressTransparencyReportId", reportId.toString()));

        final PressTransparencyReportGenerationStarted generationStarted = new PressTransparencyReportGenerationStarted(reportId, caseIds);
        when(eventSource.getStreamById(reportId)).thenReturn(pressTransparencyReportEventStream);
        when(aggregateService.get(pressTransparencyReportEventStream, PressTransparencyReportAggregate.class)).thenReturn(pressTransparencyReportAggregate);
        when(pressTransparencyReportAggregate.startTransparencyReportGeneration(eq(caseIds)))
                .thenReturn(Stream.of(generationStarted));

        pressTransparencyReportHandler.storePressTransparencyReportMetadata(storePressTransparencyReportDataCommandJsonEnvelope);
        assertThat(pressTransparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(storePressTransparencyReportDataCommandJsonEnvelope)
                                        .withName(PressTransparencyReportGenerationStarted.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.pressTransparencyReportId", equalTo(reportId.toString())),
                                        withJsonPath("$.caseIds", equalTo(caseIds.stream().map(e -> e.toString()).collect(toList()))
                                        ))))

                )));
    }

    @Test
    public void shouldHandleUpdatePressTransparencyReportDataCommand() {
        assertThat(pressTransparencyReportHandler, isHandler(COMMAND_HANDLER)
                .with(method("updatePressTransparencyReportData")
                        .thatHandles(UPDATE_PRESS_TRANSPARENCY_REPORT_DATA_COMMAND)
                ));
    }

    @Test
    public void shouldUpdateTransparencyReport() throws EventStreamException {
        final UUID reportId = randomUUID();
        final ReportMetadata reportMetadata = new ReportMetadata("press-transparency-report.pdf", 3, 1744, randomUUID());
        final JsonObject reportMetadataJsonObject = buildFileMetadataJsonObject(reportMetadata);

        final JsonEnvelope updateReportDataCommandJsonEnvelope = envelopeFrom(
                metadataWithRandomUUID(UPDATE_PRESS_TRANSPARENCY_REPORT_DATA_COMMAND),
                createObjectBuilder()
                        .add("pressTransparencyReportId", reportId.toString())
                        .add("metadata", reportMetadataJsonObject)
        );

        final PressTransparencyReportMetadataAdded metadataAdded = new PressTransparencyReportMetadataAdded(reportId, reportMetadata);
        when(eventSource.getStreamById(reportId)).thenReturn(pressTransparencyReportEventStream);
        when(aggregateService.get(pressTransparencyReportEventStream, PressTransparencyReportAggregate.class)).thenReturn(pressTransparencyReportAggregate);
        when(pressTransparencyReportAggregate.updateMetadata(eq(reportMetadataJsonObject))).thenReturn(Stream.of(metadataAdded));

        pressTransparencyReportHandler.updatePressTransparencyReportData(updateReportDataCommandJsonEnvelope);
        assertThat(pressTransparencyReportEventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(updateReportDataCommandJsonEnvelope)
                                        .withName(PressTransparencyReportMetadataAdded.EVENT_NAME),
                                payloadIsJson(allOf(
                                        withJsonPath("$.pressTransparencyReportId", equalTo(reportId.toString())),
                                        withJsonPath("$.metadata.fileName", equalTo("press-transparency-report.pdf")),
                                        withJsonPath("$.metadata.numberOfPages", equalTo(3)),
                                        withJsonPath("$.metadata.fileSize", equalTo(1744)),
                                        withJsonPath("$.metadata.fileId", equalTo(reportMetadata.getFileId().toString()))
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