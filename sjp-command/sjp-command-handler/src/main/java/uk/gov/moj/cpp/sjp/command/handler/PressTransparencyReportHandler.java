package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.DELTA;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.FULL;

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
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonString;

@ServiceComponent(Component.COMMAND_HANDLER)
public class PressTransparencyReportHandler {

    private static final String PRESS_TRANSPARENCY_REPORT_ID = "pressTransparencyReportId";
    private static final String FORMAT = "format";

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
    public void requestPressTransparencyReport(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final String documentFormat = payload.getString(FORMAT);
        final String language = payload.getString("language");
        final String documentType = payload.containsKey("requestType") ? payload.getString("requestType") : "ALL";
        initiatePressTransparencyReport(envelope, documentFormat, documentType, language);
    }

    private void initiatePressTransparencyReport(final JsonEnvelope envelope, final String documentFormat, final String documentType, final String language) throws EventStreamException {

        if ("ALL".equals(documentType)) {
            final UUID reportIdDelta = randomUUID();
            applyToPressTransparencyReportAggregate(
                    reportIdDelta,
                    envelope,
                    aggregate -> aggregate.requestPressTransparencyReport(reportIdDelta, documentFormat, DELTA.name(), language, clock.now()));
            final UUID reportIdFull = randomUUID();
            applyToPressTransparencyReportAggregate(
                    reportIdFull,
                    envelope,
                    aggregate -> aggregate.requestPressTransparencyReport(reportIdFull, documentFormat, FULL.name(), language, clock.now()));
        } else {
            final UUID reportId = randomUUID();
            applyToPressTransparencyReportAggregate(
                    reportId,
                    envelope,
                    aggregate -> aggregate.requestPressTransparencyReport(reportId, documentFormat, documentType, language, clock.now()));
        }
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
        final JsonObject payload = storePressTransparencyCommand.payloadAsJsonObject();
        final UUID reportId = getReportId(payload);
        final List<UUID> caseIds = getCaseIds(payload)
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .map(UUID::fromString)
                .collect(toList());
        applyToPressTransparencyReportAggregate(
                reportId,
                storePressTransparencyCommand,
                pressTransparencyReportAggregate -> pressTransparencyReportAggregate.startTransparencyReportGeneration(caseIds, payload));
    }

    private JsonArray getCaseIds(final JsonObject payload) {
        return payload.getJsonArray("caseIds");
    }

    private UUID getReportId(final JsonObject payload) {
        return fromString(payload.getString(PRESS_TRANSPARENCY_REPORT_ID));
    }

    @Handles("sjp.command.update-press-transparency-report-data")
    public void updatePressTransparencyReportData(final JsonEnvelope updateTransparencyReportCommand) throws EventStreamException {
        final JsonObject commandPayload = updateTransparencyReportCommand.payloadAsJsonObject();
        final UUID pressTransparencyReportId = getReportId(commandPayload);
        final JsonObject metadata = commandPayload.getJsonObject("metadata");
        applyToPressTransparencyReportAggregate(
                pressTransparencyReportId,
                updateTransparencyReportCommand,
                transparencyReportAggregate -> transparencyReportAggregate.updateMetadata(metadata));
    }

    @Handles("sjp.command.press-transparency-report-failed")
    public void pressTransparencyReportFailed(final JsonEnvelope envelope) throws EventStreamException {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID pressTransparencyReportId = getReportId(payload);
        applyToPressTransparencyReportAggregate(
                pressTransparencyReportId,
                envelope,
                PressTransparencyReportAggregate::pressTransparencyReportFailed);
    }
}
