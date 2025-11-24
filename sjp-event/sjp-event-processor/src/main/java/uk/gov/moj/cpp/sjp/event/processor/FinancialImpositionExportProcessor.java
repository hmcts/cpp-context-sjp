package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAdded;
import uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification.EnforcementNotificationService;

import javax.inject.Inject;

@ServiceComponent(EVENT_PROCESSOR)
public class FinancialImpositionExportProcessor {

    @Inject
    private Sender sender;

    @Inject
    private EnforcementNotificationService enforcementNotificationService;

    @Inject
    private JsonObjectToObjectConverter  jsonObjectToObjectConverter;

    @Handles(FinancialImpositionAccountNumberAdded.EVENT_NAME)
    public void financialImpositionAccountNumberAdded(final JsonEnvelope jsonEnvelope) {
        sender.send(envelopeFrom(
                metadataFrom(jsonEnvelope.metadata()).withName("public.sjp.financial-imposition-account-number-added"),
                jsonEnvelope.payloadAsJsonObject()
        ));

        enforcementNotificationService.checkIfEnforcementToBeNotified(fromString(jsonEnvelope.payloadAsJsonObject().getString("caseId")), jsonEnvelope);

    }
}
