package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.PersonInfoRemoved;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class RemovePersonInfoHandler {

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.command.remove-person-info")
    public void removePersonInfo(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();

        final UUID personId = UUID.fromString(payload.getString("personId"));
        final UUID caseId = UUID.fromString(payload.getString("caseId"));
        final UUID personInfoId = UUID.fromString(payload.getString("personInfoId"));

        final PersonInfoRemoved event = new PersonInfoRemoved(personInfoId, caseId, personId);

        eventSource.getStreamById(personId)
                .append(Stream.of(event)
                        .map(enveloper.withMetadataFrom(command)));
    }

}
