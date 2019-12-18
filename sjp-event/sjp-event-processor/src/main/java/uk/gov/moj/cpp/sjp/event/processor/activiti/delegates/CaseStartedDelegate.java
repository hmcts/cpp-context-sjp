package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class CaseStartedDelegate extends AbstractCaseDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseStartedDelegate.class);

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, boolean processMigration) {
        LOGGER.warn("This flow is not supported for new instances");
    }
}