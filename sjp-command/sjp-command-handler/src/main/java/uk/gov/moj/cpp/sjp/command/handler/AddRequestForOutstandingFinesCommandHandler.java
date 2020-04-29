package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.core.enveloper.Enveloper.toEnvelopeWithMetadataFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.AddRequestForOutstandingFines;
import uk.gov.moj.cpp.sjp.event.OutstandingFinesRequested;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_HANDLER)
public class AddRequestForOutstandingFinesCommandHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(AddRequestForOutstandingFinesCommandHandler.class.getName());


    @Inject
    protected EventSource eventSource;
    @Inject
    protected Enveloper enveloper;
    @Inject
    protected AggregateService aggregateService;
    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Handles("sjp.command.add-request-for-outstanding-fines")
    public void addRequestForOutstandingFines(final JsonEnvelope envelope) throws EventStreamException {

        final AddRequestForOutstandingFines addRequestForOutstandingFines = convertToObject(envelope, AddRequestForOutstandingFines.class);

        LOGGER.info("sjp.command.add-request-for-outstanding-fines {}", addRequestForOutstandingFines.getHearingDate());

        final EventStream eventStream = eventSource.getStreamById(UUID.randomUUID());

        final Stream<JsonEnvelope> newEvents =
                Stream.of(OutstandingFinesRequested.newBuilder()
                        .withHearingDate(addRequestForOutstandingFines.getHearingDate())
                        .build())
                        .map(toEnvelopeWithMetadataFrom(envelope));
        eventStream.append(newEvents);
    }

    protected <T> T convertToObject(final JsonEnvelope envelope, final Class<T> clazz) {
        return this.jsonObjectToObjectConverter.convert(envelope.payloadAsJsonObject(), clazz);
    }

}