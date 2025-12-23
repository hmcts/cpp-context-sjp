package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.timers;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataFromString;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class SendExpirationCommandDelegate implements JavaDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendExpirationCommandDelegate.class);

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Override
    public void execute(final DelegateExecution execution) {
        final UUID caseId = fromString(execution.getProcessBusinessKey());
        final String commandToSend = execution.getVariable("commandToSend", String.class);
        LOGGER.info("For case: {}, sending expiration timer command {}", caseId, commandToSend);

        final String metadataAsString = execution.getVariable(METADATA_VARIABLE, String.class);
        final Metadata metadata = metadataFromString(metadataAsString);
        final JsonObject payload = createObjectBuilder().add("caseId", caseId.toString()).build();

        sender.sendAsAdmin(envelopeFrom(metadataFrom(metadata).withName(commandToSend), payload));
    }

    public void setSender(final Sender sender) {
        this.sender = sender;
    }
}
