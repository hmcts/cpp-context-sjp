package uk.gov.moj.cpp.sjp.event.processor.service;

import uk.gov.justice.services.common.configuration.Value;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class CaseAssignmentConfiguration {

    @Inject
    @Value(key = "assignmentCandidatesLimit", defaultValue = "5")
    private String assignmentCandidatesLimitAsString;

    @Inject
    private ProsecutingAuthoritiesAssignmentsRulesLoader prosecutingAuthoritiesAssignmentsRulesLoader;

    private int assignmentCandidatesLimit;

    private ProsecutingAuthoritiesAssignmentsRules prosecutingAuthoritiesAssignmentsRules;

    @PostConstruct
    private void readConfiguration() throws IOException {
        prosecutingAuthoritiesAssignmentsRules = prosecutingAuthoritiesAssignmentsRulesLoader.load();
        assignmentCandidatesLimit = Integer.parseInt(assignmentCandidatesLimitAsString);
    }

    public ProsecutingAuthoritiesAssignmentsRules getProsecutingAuthoritiesAssignmentRules() {
        return prosecutingAuthoritiesAssignmentsRules;
    }

    public int getAssignmentCandidatesLimit() {
        return assignmentCandidatesLimit;
    }


}
