package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.METADATA_VARIABLE;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.utils.MetadataHelper;

import java.util.UUID;

import javax.inject.Inject;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractCaseDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseCompletedDelegate.class);

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    protected Sender sender;

    @Inject
    private MetadataHelper metadataHelper;

    public abstract void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution);

    @Override
    public void execute(DelegateExecution execution) {
        LOGGER.info("{} started for process {}" + execution.getCurrentActivityName(), execution.getProcessInstanceId());

        final UUID caseId = UUID.fromString(execution.getProcessBusinessKey());
        final String metadataAsString = execution.getVariable(METADATA_VARIABLE, String.class);
        final Metadata metadata = metadataHelper.metadataFromString(metadataAsString);

        execute(caseId, metadata, execution);

        LOGGER.info("{} ended for process {}" + execution.getCurrentActivityName(), execution.getProcessInstanceId());
    }

}
