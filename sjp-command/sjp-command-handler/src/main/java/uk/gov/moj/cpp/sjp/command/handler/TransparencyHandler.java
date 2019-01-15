package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.sjp.domain.aggregate.TransparencyReportAggregate.TRANSPARENCY_REPORT_STREAM_ID;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.TransparencyReportAggregate;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;

@ServiceComponent(Component.COMMAND_HANDLER)
public class TransparencyHandler {

    @Inject
    private Clock clock;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.command.request-transparency-report")
    public void requestTransparencyReport(final JsonEnvelope requestTransparencyReportCommand) throws EventStreamException {
        applyToTransparencyReportAggregate(requestTransparencyReportCommand,
                transparencyReportAggregate -> transparencyReportAggregate.requestTransparencyReport(clock.now()));
    }

    @Handles("sjp.command.store-transparency-report-data")
    public void storeTransparencyReportData(final JsonEnvelope storeTransparencyReportDataCommand) throws EventStreamException {
        final JsonObject storeTransparencyReportPayload = storeTransparencyReportDataCommand.payloadAsJsonObject();
        final List<UUID> caseIds = storeTransparencyReportPayload.getJsonArray("caseIds")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .map(UUID::fromString)
                .collect(toList());
        final JsonObject englishReportMetadata = storeTransparencyReportPayload.getJsonObject("englishReportMetadata");
        final JsonObject welshReportMetadata = storeTransparencyReportPayload.getJsonObject("welshReportMetadata");

        applyToTransparencyReportAggregate(storeTransparencyReportDataCommand,
                transparencyReportAggregate -> transparencyReportAggregate.generateTransparencyReport(caseIds, englishReportMetadata, welshReportMetadata));
    }

    private void applyToTransparencyReportAggregate(final JsonEnvelope transparencyReportCommand, final Function<TransparencyReportAggregate, Stream<Object>> function) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(TRANSPARENCY_REPORT_STREAM_ID);
        final TransparencyReportAggregate transparencyReportAggregate = aggregateService.get(eventStream, TransparencyReportAggregate.class);
        final Stream<Object> events = function.apply(transparencyReportAggregate);

        eventStream.append(events.map(enveloper.withMetadataFrom(transparencyReportCommand)));
    }
}
