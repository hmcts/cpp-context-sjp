package uk.gov.moj.cpp.sjp.event.processor.service.assignment;

import uk.gov.justice.services.common.configuration.Value;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class AssignmentConfiguration {

    @Inject
    @Value(key = "assignmentCandidatesLimit", defaultValue = "5")
    private String assignmentCandidatesLimitAsString;

    @Inject
    private AssignmentRulesLoader assignmentRulesLoader;

    private int assignmentCandidatesLimit;

    private AssignmentRules assignmentRules;

    @PostConstruct
    private void readConfiguration() throws IOException {
        assignmentRules = assignmentRulesLoader.load();
        assignmentCandidatesLimit = Integer.parseInt(assignmentCandidatesLimitAsString);
    }

    public AssignmentRules getAssignmentRules() {
        return assignmentRules;
    }

    public int getAssignmentCandidatesLimit() {
        return assignmentCandidatesLimit;
    }


}
