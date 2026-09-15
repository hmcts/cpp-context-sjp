package uk.gov.moj.cpp.sjp.event.processor.service.assignment;

import uk.gov.justice.services.common.configuration.Value;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AssignmentConfiguration {

    @Inject
    @Value(key = "assignmentCandidatesLimit", defaultValue = "5")
    private String assignmentCandidatesLimitAsString;

    private int assignmentCandidatesLimit;

    @PostConstruct
    private void readConfiguration() {
        assignmentCandidatesLimit = Integer.parseInt(assignmentCandidatesLimitAsString);
    }

    public int getAssignmentCandidatesLimit() {
        return assignmentCandidatesLimit;
    }


}
