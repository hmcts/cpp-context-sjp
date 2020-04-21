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

    private int assignmentCandidatesLimit;

    @PostConstruct
    private void readConfiguration() throws IOException {
        assignmentCandidatesLimit = Integer.parseInt(assignmentCandidatesLimitAsString);
    }

    public int getAssignmentCandidatesLimit() {
        return assignmentCandidatesLimit;
    }


}
