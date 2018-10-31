package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.PROCESS_MIGRATION_VARIABLE;
import static uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper.metadataFromString;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCaseDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCaseDelegate.class);

    @Inject
    protected Clock clock;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    protected Sender sender;

    public abstract void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, boolean processMigration);

    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("{} started for process {}", execution.getCurrentActivityName(), execution.getProcessInstanceId());

        final UUID caseId = UUID.fromString(execution.getProcessBusinessKey());
        final String metadataAsString = execution.getVariable(METADATA_VARIABLE, String.class);
        final Metadata metadata = metadataFromString(metadataAsString);
        final boolean processMigration = execution.hasVariable(PROCESS_MIGRATION_VARIABLE);

        if (processMigration) {
            execution.removeVariable(PROCESS_MIGRATION_VARIABLE);
        }

        execute(caseId, metadata, execution, processMigration);

        LOGGER.info("{} ended for process {}", execution.getCurrentActivityName(), execution.getProcessInstanceId());
    }

    protected void sendAsAdmin(final Metadata metadata, final String metadataName, final JsonObject payload) {
        sender.sendAsAdmin(envelopeFrom(
                metadataFrom(metadata).withName(metadataName).build(),
                payload));
    }

}
