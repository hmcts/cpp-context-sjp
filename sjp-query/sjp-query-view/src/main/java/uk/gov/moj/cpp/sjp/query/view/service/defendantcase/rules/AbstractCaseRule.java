package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules;

import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCase;

public abstract class AbstractCaseRule {

    private final CaseRuleType ruleType;

    public AbstractCaseRule(CaseRuleType ruleType) {
        this.ruleType = ruleType;
    }

    public CaseRuleType getRuleType() {
        return ruleType;
    }

    public abstract CaseRuleResult executeRule(DefendantCase defendantCase);
}
