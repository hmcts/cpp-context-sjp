package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Case;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class CreateCaseHandler extends CaseCommandHandler {

    static final String FIELD_STREAM_ID = "id";

    @Inject
    private Clock clock;

    @Handles("sjp.command.create-sjp-case")
    public void createSjpCase(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command, aCase -> aCase.createCase(converter.convert(command.payloadAsJsonObject(), Case.class), clock.now()));
    }

    @Override
    protected UUID getCaseId(final JsonObject payload) {
        return UUID.fromString(payload.getString(FIELD_STREAM_ID));
    }

}
