package uk.gov.moj.cpp.sjp.event.processor.service.assignment;

import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static uk.gov.moj.cpp.sjp.domain.AssignmentRuleType.DISALLOW;

import java.util.ArrayList;
import java.util.Collection;

public class AssignmentRules {

    private static final AssignmentRule DISALLOW_NONE_RULE = new AssignmentRule(EMPTY, emptySet(), DISALLOW);

    private final Collection<AssignmentRule> rules;

    public AssignmentRules(final Collection<AssignmentRule> rules) {
        this.rules = new ArrayList<>(rules);
    }

    /**
     * @return AssignmentRule that has longest match of courtHouseCodePrefix with provided
     * courtHouseCode or rule that disallow none prosecuting authority
     */
    public AssignmentRule getBestCaseAssignmentRule(final String courtHouseCode) {
        return rules
                .stream()
                .filter(assignmentRule -> courtHouseCode.startsWith(assignmentRule.getCourtHouseCodePrefix()))
                .max(comparing(assignmentRule -> assignmentRule.getCourtHouseCodePrefix().length()))
                .orElse(DISALLOW_NONE_RULE);
    }

    public Collection<AssignmentRule> getAssignmentRules() {
        return new ArrayList<>(rules);
    }
}
