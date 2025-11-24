package uk.gov.moj.cpp.sjp.query.view.service.defendantcase.rules;

import static uk.gov.moj.cpp.sjp.query.view.service.ProgressionService.CASE_STATUS_FIELD_NAME;
import static uk.gov.moj.cpp.sjp.query.view.service.ProgressionService.PROSECUTION_CASE_FIELD_NAME;

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
 * This rule determines if Court Case is in open status. Rule for a case is:
 *  - if referred by SJP, pick case status from prosecution case
 *  - if not referred, pick case status from case
 *  - Valid case status is not to be INACTIVE and CLOSED
 */
public class CourtCaseOpenRule extends AbstractCaseRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourtCaseOpenRule.class);

    private final DefendantPotentialCaseServiceImpl defendantCaseService;

    public CourtCaseOpenRule(final DefendantPotentialCaseServiceImpl defendantCaseService) {
        super(CaseRuleType.COURT_CASE_OPEN);
        this.defendantCaseService = defendantCaseService;
    }

    @Override
    public CaseRuleResult executeRule(final DefendantCase defendantCase) {
        final UUID caseId = defendantCase.getCaseId();
        final boolean courtCaseOpen;
        String caseStatus = "";
        if (defendantCase.isSjp()) {
            final Optional<JsonObject> progressionCaseOpt =
                                               defendantCaseService.findProgressionCaseById(caseId);
            if (progressionCaseOpt.isPresent()) {
                final JsonObject prosecutionCase = progressionCaseOpt.get().
                                                        getJsonObject(PROSECUTION_CASE_FIELD_NAME);
                if (prosecutionCase != null) {
                    caseStatus = prosecutionCase.getString(CASE_STATUS_FIELD_NAME);
                    LOGGER.debug("Defendant case is Sjp originated and progressionCaseStatus={} - " +
                                 "caseId={}", caseStatus, caseId);
                }
            }
        } else {
            caseStatus = defendantCase.getCaseStatus();
        }
        courtCaseOpen = StringUtils.isNoneEmpty(caseStatus)
                            && !Objects.equals(caseStatus, CaseRuleUtils.CC_INACTIVE_STATUS)
                            && !Objects.equals(caseStatus, CaseRuleUtils.CC_CLOSED_STATUS);
        LOGGER.debug("Execution of {} rule - matched={} caseId={}",
                     getRuleType(), courtCaseOpen, defendantCase.getCaseId());

        return new CaseRuleResult(getRuleType(), courtCaseOpen);
    }
}
