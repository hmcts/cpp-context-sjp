package uk.gov.moj.cpp.sjp.event.processor.activiti.delegates;


import static java.util.UUID.fromString;
import static org.slf4j.LoggerFactory.getLogger;

import uk.gov.moj.cpp.sjp.event.processor.service.assignment.AssignmentService;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.slf4j.Logger;

@Named
public class AssignmentTimedOutDelegate implements JavaDelegate {

    private static final Logger LOGGER = getLogger(AssignmentTimedOutDelegate.class);

    @Inject
    private AssignmentService assignmentService;

    @Override
    public void execute(final DelegateExecution execution) {

        final UUID caseId = fromString(execution.getProcessBusinessKey());

        assignmentService.unassignCase(caseId);

        LOGGER.info("Case assignment timeout ended for case {} ", caseId);
    }
}