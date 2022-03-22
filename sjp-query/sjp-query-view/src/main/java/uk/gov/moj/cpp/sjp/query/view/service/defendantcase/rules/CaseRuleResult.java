package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules;

public class CaseRuleResult {

    private final CaseRuleType ruleType;
    private final boolean match;

    public CaseRuleResult(CaseRuleType ruleType,
                          boolean match) {
        this.ruleType = ruleType;
        this.match = match;
    }

    public boolean isMatch() {
        return match;
    }

    public CaseRuleType getRuleType() {
        return ruleType;
    }

    @Override
    public String toString() {
        return "RuleResult{" +
                "ruleType=" + ruleType +
                ", match=" + match +
                '}';
    }
}
