package uk.gov.moj.cpp.sjp.command.handler;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(Component.COMMAND_HANDLER)
public class AddDatesToAvoidHandler extends CaseCommandHandler {

    @Inject
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Handles("sjp.command.add-dates-to-avoid")
    public void addDatesToAvoid(final JsonEnvelope command) throws EventStreamException {
        final JsonObject payload = command.payloadAsJsonObject();
        final String datesToAvoid = payload.getString("datesToAvoid");
        final ProsecutingAuthorityAccess prosecutingAuthorityAccess = prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(command);
        applyToCaseAggregate(command, aggregate -> aggregate.addDatesToAvoid(datesToAvoid, prosecutingAuthorityAccess.getProsecutingAuthority()));
    }

    @Handles("sjp.command.expire-dates-to-avoid-timer")
    public void expireDatesToAvoidTimer(final JsonEnvelope command) throws EventStreamException {
        applyToCaseAggregate(command, CaseAggregate::expireDatesToAvoidTimer);
    }
}
