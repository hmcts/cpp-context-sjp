package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.plea.SetPleas;

import javax.inject.Inject;
import javax.json.JsonObject;
import java.util.UUID;

@ServiceComponent(Component.COMMAND_HANDLER)
public class SetPleasCommandHandler extends CaseCommandHandler {

    public static final String COMMAND_NAME = "sjp.command.set-pleas";

    @Inject
    private Clock clock;

    @Handles(COMMAND_NAME)
    public void setPleas(final JsonEnvelope setPleasCommand) throws EventStreamException {
        // TODO: DO WE NEED TO HANDLE plea_cancelled?

        final JsonObject payload = setPleasCommand.payloadAsJsonObject();

        final UUID caseId = getCaseId(payload);
        final UUID userId = getUserId(setPleasCommand);
        final SetPleas pleas = converter.convert(payload, SetPleas.class);

        applyToCaseAggregate(setPleasCommand, aCase -> aCase.setPleas(caseId, pleas, userId, clock.now()));

    }

}
