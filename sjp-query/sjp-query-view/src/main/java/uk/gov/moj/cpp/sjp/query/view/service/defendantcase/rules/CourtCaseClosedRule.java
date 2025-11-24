package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules;

import static uk.gov.moj.cpp.sjp.query.view.service.ProgressionService.CASE_STATUS_FIELD_NAME;
import static uk.gov.moj.cpp.sjp.query.view.service.ProgressionService.PROSECUTION_CASE_FIELD_NAME;
import static uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules.CaseRuleUtils.lastHearingDateWithinRange;

import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.DefendantPotentialCaseServiceImpl;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.search.DefendantCase;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This rule determines if Court Case is in closed status. Rule for a case is:
 *  - if referred by SJP, pick case status from prosecution case
 *  - if not referred, pick case status from case
 *  - Valid case status is either INACTIVE or CLOSED
 *  - Last hearing date was within last 28 days
 */
public class CourtCaseClosedRule extends AbstractCaseRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtCaseClosedRule.class);

    private final DefendantPotentialCaseServiceImpl defendantCaseService;

    public CourtCaseClosedRule(final DefendantPotentialCaseServiceImpl defendantCaseService) {
        super(CaseRuleType.COURT_CASE_CLOSED);
        this.defendantCaseService = defendantCaseService;
    }

    @Override
    public CaseRuleResult executeRule(final DefendantCase defendantCase) {
        final UUID caseId = defendantCase.getCaseId();
        final boolean courtCaseClosedWithin28Days;
        String caseStatus = "";
        if (defendantCase.isSjp()) {
            final Optional<JsonObject> progressionCaseOpt =
                                               defendantCaseService.findProgressionCaseById(caseId);
            if (progressionCaseOpt.isPresent()) {
                final JsonObject prosecutionCase = progressionCaseOpt.get().
                                                            getJsonObject(PROSECUTION_CASE_FIELD_NAME);
                if (prosecutionCase != null) {
                    caseStatus = prosecutionCase.getString(CASE_STATUS_FIELD_NAME);
                    LOGGER.debug("Defendant case is Sjp originated and progressionCaseStatus={} - caseId={}",
                            caseStatus, caseId);
                }
            }
        } else {
            caseStatus = defendantCase.getCaseStatus();
        }
        courtCaseClosedWithin28Days = StringUtils.isNoneEmpty(caseStatus)
                                      && (Objects.equals(caseStatus, CaseRuleUtils.CC_INACTIVE_STATUS)
                                                || Objects.equals(caseStatus, CaseRuleUtils.CC_CLOSED_STATUS))
                                      && lastHearingDateWithinRange(defendantCase.getHearings());
        LOGGER.debug("Execution of {} rule - matched={} caseId={}",
                     getRuleType(), courtCaseClosedWithin28Days, defendantCase.getCaseId());

        return new CaseRuleResult(getRuleType(), courtCaseClosedWithin28Days);
    }
}
