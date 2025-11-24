package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules;

import static uk.gov.moj.cpp.sjp.domain.common.CaseStatus.COMPLETED;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleUtils.dateWithinRange;

import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.DefendantPotentialCaseServiceImpl;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCase;

import java.time.LocalDate;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This rule determines if SJP session is in closed status. Rule for a case is:
 *  - SJP flag is set
 *  - Valid case status is Completed
 *  - Decision date was made within last 28 days
 */
public class SjpClosedRule extends AbstractCaseRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(SjpClosedRule.class);

    private final DefendantPotentialCaseServiceImpl defendantCaseService;

    public SjpClosedRule(DefendantPotentialCaseServiceImpl defendantCaseService) {
        super(CaseRuleType.SJP_CLOSED);
        this.defendantCaseService = defendantCaseService;
    }

    @Override
    public CaseRuleResult executeRule(final DefendantCase defendantCase) {
        final UUID caseId = defendantCase.getCaseId();
        boolean sjpClosedWithin28Days = false;
        if (defendantCase.isSjp()) {
            final CaseStatus caseStatus = CaseStatus.fromName(defendantCase.getCaseStatus());
            if (caseStatus == COMPLETED) {
                final CaseDecision caseDecision = defendantCaseService.findCaseDecisionById(caseId);
                if (caseDecision != null && caseDecision.getSavedAt() != null) {
                    final LocalDate decisionDate = caseDecision.getSavedAt().toLocalDate();
                    LOGGER.debug("Checking whether final decision date={} within range - caseId={}",
                                 decisionDate, caseId);
                    sjpClosedWithin28Days = dateWithinRange(decisionDate);
                } else {
                    LOGGER.warn("Skipping sjp closed rule, no case decision found to check " +
                                "decision date - caseId={}", caseId);
                }
            }
        }
        LOGGER.debug("Execution of {} rule - matched={} caseId={}",
                     getRuleType(), sjpClosedWithin28Days, defendantCase.getCaseId());

        return new CaseRuleResult(getRuleType(), sjpClosedWithin28Days);
    }
}
