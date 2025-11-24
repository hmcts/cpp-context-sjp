package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseManagementStatusAggregate.CASE_MANAGEMENT_STATUS_STREAM_ID;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseManagementStatusAggregate;

import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class UpdateCasesManagementStatusHandler {
    @Inject
    private AggregateService aggregateService;

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    public static final String UPDATE_CASES_MANAGEMENT_STATUS = "sjp.command.update-cases-management-status";


    @Handles(UPDATE_CASES_MANAGEMENT_STATUS)
    public void updateCasesManagementStatus(final JsonEnvelope updateCasesManagementStatusCommand) throws EventStreamException {
        final JsonObject payload = updateCasesManagementStatusCommand.payloadAsJsonObject();
        applyToCaseManagementStatusAggregate(updateCasesManagementStatusCommand,
                caseManagementStatusAggregate -> caseManagementStatusAggregate.updateCaseManagementStatus(payload));
    }

    private void applyToCaseManagementStatusAggregate(final JsonEnvelope caseManagementStatusCommand, final Function<CaseManagementStatusAggregate, Stream<Object>> function) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(CASE_MANAGEMENT_STATUS_STREAM_ID);
        final CaseManagementStatusAggregate caseManagementStatusAggregate = aggregateService.get(eventStream, CaseManagementStatusAggregate.class);
        final Stream<Object> events = function.apply(caseManagementStatusAggregate);

        eventStream.append(events.map(enveloper.withMetadataFrom(caseManagementStatusCommand)));
    }
}
