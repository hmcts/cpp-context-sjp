package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
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
import uk.gov.moj.cpp.sjp.domain.aggregate.PressTransparencyReportAggregate;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonString;

@ServiceComponent(Component.COMMAND_HANDLER)
public class PressTransparencyReportHandler {

    @Inject
    private AggregateService aggregateService;

    @Inject
    private EventSource eventSource;

    @Inject
    private Clock clock;

    @Inject
    private Enveloper enveloper;

    @Inject
    private JsonObjectToObjectConverter converter;

    @Handles("sjp.command.request-press-transparency-report")
    public void requestPressTransparencyReport(final JsonEnvelope requestPressTransparencyReportCommand) throws EventStreamException {
        final UUID reportId = requestPressTransparencyReportCommand.metadata().id();
        applyToPressTransparencyReportAggregate(
                reportId,
                requestPressTransparencyReportCommand,
                pressTransparencyReportAggregate -> pressTransparencyReportAggregate.requestPressTransparencyReport(reportId, clock.now()));
    }

    private void applyToPressTransparencyReportAggregate(
            final UUID reportId,
            final JsonEnvelope pressTransparencyReportCommand,
            final Function<PressTransparencyReportAggregate, Stream<Object>> function) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(reportId);
        final PressTransparencyReportAggregate pressTransparencyReportAggregate = aggregateService.get(eventStream, PressTransparencyReportAggregate.class);
        final Stream<Object> events = function.apply(pressTransparencyReportAggregate);

        eventStream.append(events.map(enveloper.withMetadataFrom(pressTransparencyReportCommand)));
    }

    @Handles("sjp.command.store-press-transparency-report-data")
    public void storePressTransparencyReportMetadata(final JsonEnvelope storePressTransparencyCommand) throws EventStreamException {
        final JsonObject storePressTransparencyReportPayload = storePressTransparencyCommand.payloadAsJsonObject();
        final UUID reportId = fromString(storePressTransparencyReportPayload.getString("pressTransparencyReportId"));
        final List<UUID> caseIds = storePressTransparencyReportPayload.getJsonArray("caseIds")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .map(UUID::fromString)
                .collect(toList());

        applyToPressTransparencyReportAggregate(
                reportId,
                storePressTransparencyCommand,
                pressTransparencyReportAggregate -> pressTransparencyReportAggregate.startTransparencyReportGeneration(caseIds));
    }

    @Handles("sjp.command.update-press-transparency-report-data")
    public void updatePressTransparencyReportData(final JsonEnvelope updateTransparencyReportCommand) throws EventStreamException {
        final JsonObject commandPayload = updateTransparencyReportCommand.payloadAsJsonObject();
        final UUID pressTransparencyReportId = fromString(commandPayload.getString("pressTransparencyReportId"));
        final JsonObject metadata = commandPayload.getJsonObject("metadata");
        applyToPressTransparencyReportAggregate(
                pressTransparencyReportId,
                updateTransparencyReportCommand,
                transparencyReportAggregate -> transparencyReportAggregate.updateMetadata(metadata));
    }


}
