package uk.gov.moj.cpp.sjp.domain.verdict;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;

import static java.util.Objects.isNull;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;


public class VerdictService {

    @SuppressWarnings("squid:S1172")
    public VerdictType calculateVerdict(PleaType pleaType, DecisionType decision) {
        switch (decision) {
            case ADJOURN:
            case WITHDRAW:
                return NO_VERDICT;
            case REFER_FOR_COURT_HEARING:
                return calculateVerdictForReferToCourtDecision(pleaType);
            case DISMISS:
                return FOUND_NOT_GUILTY;
            case DISCHARGE:
            case FINANCIAL_PENALTY:
                return PleaType.GUILTY.equals(pleaType) ? FOUND_GUILTY : PROVED_SJP;
            default:
                throw new IllegalArgumentException(String.format("Decision %s type has not been mapped", decision));

        }
    }

    private VerdictType calculateVerdictForReferToCourtDecision(PleaType pleaType) {
        if(isNull(pleaType)) {
            return PROVED_SJP;
        } else if(PleaType.GUILTY.equals(pleaType) || PleaType.GUILTY_REQUEST_HEARING.equals(pleaType)) {
            return FOUND_GUILTY;
        }
        throw new IllegalArgumentException(String.format("PleaType %s has not been mapped", pleaType));
    }

}
