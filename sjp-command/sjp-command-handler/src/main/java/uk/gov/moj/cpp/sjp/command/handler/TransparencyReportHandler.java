package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.DELTA;
import static uk.gov.moj.cpp.sjp.domain.DocumentRequestType.FULL;

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
public class TransparencyReportHandler {

    private static final String TRANSPARENCY_REPORT_ID = "transparencyReportId";
    private static final String FORMAT = "format";
    private static final String TEMPLATE_IDENTIFIER = "templateIdentifier";

    @Inject
    private Clock clock;

    @Inject
    private AggregateService aggregateService;

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.command.request-transparency-report")
    public void requestTransparencyReport(final JsonEnvelope envelope) throws EventStreamException {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final String documentFormat = payload.getString(FORMAT);
        final String language = payload.getString("language");
        final String documentType = payload.containsKey("requestType") ? payload.getString("requestType") : "ALL";
        initiatePressTransparencyReport(envelope, documentFormat, documentType, language);
    }

    private void initiatePressTransparencyReport(final JsonEnvelope envelope, final String documentFormat, final String documentType, final String language) throws EventStreamException {

        if ("ALL".equals(documentType)) {
            final UUID reportIdDelta = randomUUID();
            applyToTransparencyReportAggregate(
                    reportIdDelta,
                    envelope,
                    aggregate -> aggregate.requestTransparencyReport(reportIdDelta, documentFormat, DELTA.name(), language, clock.now()));
            final UUID reportIdFull = randomUUID();
            applyToTransparencyReportAggregate(
                    reportIdFull,
                    envelope,
                    aggregate -> aggregate.requestTransparencyReport(reportIdFull, documentFormat, FULL.name(), language, clock.now()));
        } else {
            final UUID reportId = randomUUID();
            applyToTransparencyReportAggregate(
                    reportId,
                    envelope,
                    aggregate -> aggregate.requestTransparencyReport(reportId, documentFormat, documentType, language, clock.now()));
        }
    }

    @Handles("sjp.command.store-transparency-report-data")
    public void storeTransparencyReportData(final JsonEnvelope storeTransparencyReportDataCommand) throws EventStreamException {

        final JsonObject payload = storeTransparencyReportDataCommand.payloadAsJsonObject();
        final List<UUID> caseIds = payload.getJsonArray("caseIds")
                .getValuesAs(JsonString.class)
                .stream()
                .map(JsonString::getString)
                .map(UUID::fromString)
                .collect(toList());
        final UUID transparencyReportId = fromString(payload.getString(TRANSPARENCY_REPORT_ID));
        applyToTransparencyReportAggregate(
                transparencyReportId,
                storeTransparencyReportDataCommand,
                transparencyReportAggregate -> transparencyReportAggregate.startTransparencyReportGeneration(caseIds, payload));
    }

    @Handles("sjp.command.update-transparency-report-data")
    public void updateTransparencyReportData(final JsonEnvelope updateTransparencyReportCommand) throws EventStreamException {

        final JsonObject updateTransparencyReportPayload = updateTransparencyReportCommand.payloadAsJsonObject();
        final UUID transparencyReportId = fromString(updateTransparencyReportPayload.getString(TRANSPARENCY_REPORT_ID));
        final JsonObject metadata = updateTransparencyReportPayload.getJsonObject("metadata");
        final String language = updateTransparencyReportPayload.getString("language");
        applyToTransparencyReportAggregate(
                transparencyReportId,
                updateTransparencyReportCommand,
                transparencyReportAggregate -> transparencyReportAggregate.updateMetadataForLanguage(language, metadata));
    }

    @Handles("sjp.command.transparency-report-failed")
    public void transparencyReportFailed(final JsonEnvelope envelope) throws EventStreamException {

        final JsonObject payload = envelope.payloadAsJsonObject();
        final UUID transparencyReportId = fromString(payload.getString(TRANSPARENCY_REPORT_ID));
        final String templateIdentifier = payload.getString(TEMPLATE_IDENTIFIER);
        applyToTransparencyReportAggregate(
                transparencyReportId,
                envelope,
                transparencyReportAggregate -> transparencyReportAggregate.transparencyReportFailed(templateIdentifier));
    }

    private void applyToTransparencyReportAggregate(final UUID streamId,
                                                    final JsonEnvelope transparencyReportCommand,
                                                    final Function<TransparencyReportAggregate, Stream<Object>> function) throws EventStreamException {

        final EventStream eventStream = eventSource.getStreamById(streamId);
        final TransparencyReportAggregate transparencyReportAggregate = aggregateService.get(eventStream, TransparencyReportAggregate.class);
        final Stream<Object> events = function.apply(transparencyReportAggregate);
        eventStream.append(events.map(enveloper.withMetadataFrom(transparencyReportCommand)));
    }
}
