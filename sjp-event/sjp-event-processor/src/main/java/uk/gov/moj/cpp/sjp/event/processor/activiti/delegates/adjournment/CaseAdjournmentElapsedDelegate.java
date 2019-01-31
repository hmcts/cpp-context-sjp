package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.adjournment;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.activiti.CaseStateService.CASE_ADJOURNED_VARIABLE;

import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.event.processor.activiti.delegates.AbstractCaseDelegate;

import java.util.UUID;

import javax.inject.Named;
import javax.json.JsonObject;

import org.activiti.engine.delegate.DelegateExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class CaseAdjournmentElapsedDelegate extends AbstractCaseDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseAdjournmentElapsedDelegate.class);

    @Override
    public void execute(final UUID caseId, final Metadata metadata, final DelegateExecution execution, final boolean processMigration) {
        LOGGER.info("Case adjournment period elapsed for {}", caseId);
        execution.setVariable(CASE_ADJOURNED_VARIABLE, false);

        final JsonObject payload = createObjectBuilder().add("caseId", caseId.toString()).build();
        sendAsAdmin(metadata, "sjp.command.record-case-adjournment-to-later-sjp-hearing-elapsed", payload);
    }
}
