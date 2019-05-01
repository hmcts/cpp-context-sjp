package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;

import uk.gov.justice.services.messaging.Metadata;

import java.util.UUID;

import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @deprecated For backward compatibility only. DecisionProcessor sends sjp.command.complete-case as
 * a response to public.resulting.referenced-decisions-saved.
 */
@Deprecated
@Named
public class CaseCompletedDelegate extends AbstractCaseDelegate {
    private static final Logger LOGGER = LoggerFactory.getLogger(CaseCompletedDelegate.class);

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, boolean processMigration) {
        LOGGER.info("CaseCompletedDelegate executed for legacy case {}", caseId);
    }
}