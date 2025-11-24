package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules;

import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.REFERRED_FOR_COURT_HEARING;
import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.UNKNOWN;

import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This rule determines if SJP session is in open status. Rule for a case is:
 *  - SJP flag is set
 *  - Valid case status is not to be Completed and Referred
 */
public class SjpOpenRule extends AbstractCaseRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(SjpOpenRule.class);

    public SjpOpenRule() {
        super(CaseRuleType.SJP_OPEN);
    }

    @Override
    public CaseRuleResult executeRule(final DefendantCase defendantCase) {
        boolean sjpOpen = false;
        if (defendantCase.isSjp()) {
            final CaseStatus caseStatus = CaseStatus.fromName(defendantCase.getCaseStatus());
            if (caseStatus != UNKNOWN && caseStatus != COMPLETED
                                      && caseStatus != REFERRED_FOR_COURT_HEARING) {
                sjpOpen = true;
            }
        }
        LOGGER.debug("Execution of {} rule - matched={} caseId={}",
                getRuleType(), sjpOpen, defendantCase.getCaseId());

        return new CaseRuleResult(getRuleType(), sjpOpen);
    }
}
