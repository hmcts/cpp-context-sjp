package uk.gov.moj.cpp.sjp.event.processor.service.assignment;

import uk.gov.moj.cpp.sjp.domain.AssignmentRuleType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class AssignmentRule {

    private final Set<String> prosecutingAuthorities;
    private final String courtHouseCodePrefix;
    private final AssignmentRuleType assignmentRuleType;

    public AssignmentRule(final String courtHouseCodePrefix, final Collection<String> prosecutingAuthorities, final AssignmentRuleType assignmentRuleType) {
        this.courtHouseCodePrefix = courtHouseCodePrefix;
        this.prosecutingAuthorities = new HashSet<>(prosecutingAuthorities);
        this.assignmentRuleType = assignmentRuleType;
    }

    public Set<String> getProsecutingAuthorities() {
        return new HashSet<>(prosecutingAuthorities);
    }

    public String getCourtHouseCodePrefix() {
        return courtHouseCodePrefix;
    }

    public AssignmentRuleType getAssignmentRuleType() {
        return assignmentRuleType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AssignmentRule that = (AssignmentRule) o;
        return Objects.equals(prosecutingAuthorities, that.prosecutingAuthorities) &&
                Objects.equals(courtHouseCodePrefix, that.courtHouseCodePrefix) &&
                assignmentRuleType == that.assignmentRuleType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prosecutingAuthorities, courtHouseCodePrefix, assignmentRuleType);
    }

    @Override
    public String toString() {
        return "AssignmentRule{" +
                "prosecutingAuthorities=" + prosecutingAuthorities +
                ", courtHouseCodePrefix='" + courtHouseCodePrefix + '\'' +
                ", assignmentRuleType=" + assignmentRuleType +
                '}';
    }
}
