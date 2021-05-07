package uk.gov.moj.cpp.sjp.command.handler;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.json.schemas.domains.sjp.command.SetOffencesWithdrawalRequestsStatus;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(COMMAND_HANDLER)
public class OffenceWithdrawalRequestHandler extends CaseCommandHandler {

    @Inject
    private Clock clock;

    @Inject
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("sjp.command.set-offences-withdrawal-requests-status")
    public void setOffencesWithdrawalRequestsStatus(final Envelope<SetOffencesWithdrawalRequestsStatus> command) throws EventStreamException {

        final SetOffencesWithdrawalRequestsStatus setOffencesWithdrawalRequestsStatus = command.payload();
        final JsonEnvelope jsonEnvelope = envelopeFrom(command.metadata(), objectToJsonObjectConverter.convert(command.payload()));
        final ProsecutingAuthorityAccess prosecutingAuthorityAccess = prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(jsonEnvelope);

        applyToCaseAggregate(setOffencesWithdrawalRequestsStatus.getCaseId(), command, caseAggregate -> caseAggregate.requestForOffenceWithdrawal(
                setOffencesWithdrawalRequestsStatus.getCaseId(),
                UUID.fromString(command.metadata().userId().get()),
                clock.now(),
                setOffencesWithdrawalRequestsStatus.getWithdrawalRequestsStatus(),
                prosecutingAuthorityAccess.getProsecutingAuthority()));
    }

}
