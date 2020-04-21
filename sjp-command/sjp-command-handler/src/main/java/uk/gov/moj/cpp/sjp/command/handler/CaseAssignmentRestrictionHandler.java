package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.json.schemas.domains.sjp.AddCaseAssignmentRestriction;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAssignmentRestrictionAggregate;

import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonValue;


@ServiceComponent(Component.COMMAND_HANDLER)
public class CaseAssignmentRestrictionHandler {

    // All case assignment restrictions shared the same streamId. Do not modify it!
    public static final UUID CASE_ASSIGNMENT_RESTRICTION_STREAM_ID = fromString("573018e6-f540-40be-b61c-e72b63d1ead7");

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Inject
    private Clock clock;

    @Inject
    private AggregateService aggregateService;

    @Handles("sjp.command.add-case-assignment-restriction")
    public void addCaseAssignmentRestriction(final Envelope<AddCaseAssignmentRestriction> command) throws EventStreamException {
        final AddCaseAssignmentRestriction payload = command.payload();
        applyToCaseAssignmentRestrictionAggregate(command,
                caseAssignmentRestrictionAggregate ->
                        caseAssignmentRestrictionAggregate.updateCaseAssignmentRestriction(payload.getProsecutingAuthority(),
                                payload.getIncludeOnly(),
                                payload.getExclude(),
                                clock.now().toString()));
    }

    private void applyToCaseAssignmentRestrictionAggregate(final Envelope<AddCaseAssignmentRestriction> command, final Function<CaseAssignmentRestrictionAggregate, Stream<Object>> function) throws EventStreamException {
        final EventStream eventStream = eventSource.getStreamById(CASE_ASSIGNMENT_RESTRICTION_STREAM_ID);
        final CaseAssignmentRestrictionAggregate caseAssignmentRestrictionAggregate = aggregateService.get(eventStream, CaseAssignmentRestrictionAggregate.class);
        final Stream<Object> events = function.apply(caseAssignmentRestrictionAggregate);

        final JsonEnvelope jsonEnvelope = envelopeFrom(command.metadata(), JsonValue.NULL);
        eventStream.append(events.map(enveloper.withMetadataFrom(jsonEnvelope)));
    }
}
