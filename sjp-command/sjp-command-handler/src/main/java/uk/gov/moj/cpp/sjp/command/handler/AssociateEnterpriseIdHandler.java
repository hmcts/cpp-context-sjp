package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.EnterpriseIdAssociated;

import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_HANDLER)
public class AssociateEnterpriseIdHandler {

    private static final String CASE_ID_PROPERTY = "caseId";
    private static final String ENTERPRISE_ID_PROPERTY = "enterpriseId";

    @Inject
    private EventSource eventSource;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.command.associate-enterprise-id")
    public void associateEnterpriseId(final JsonEnvelope command) throws EventStreamException {

        final JsonObject payload = command.payloadAsJsonObject();

        final UUID caseId = UUID.fromString(payload.getString(CASE_ID_PROPERTY));
        final String enterpriseId = payload.getString(ENTERPRISE_ID_PROPERTY);

        final EnterpriseIdAssociated event = new EnterpriseIdAssociated(caseId, enterpriseId);

        final EventStream eventStream = eventSource.getStreamById(caseId);
        eventStream.append(Stream.of(event).map(enveloper.withMetadataFrom(command)));
    }
}
