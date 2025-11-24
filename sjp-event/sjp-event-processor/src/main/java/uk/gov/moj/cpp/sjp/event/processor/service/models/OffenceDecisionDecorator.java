package uk.gov.moj.cpp.sjp.event.processor.service.models;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import uk.gov.justice.json.schemas.domains.sjp.queries.DisqualificationType;
import uk.gov.justice.json.schemas.domains.sjp.queries.QueryOffenceDecision;

/**
 * Because the classes used by the Sjp Query endpoints are auto-generated we cannot add any
 * behaviour that was supposed to be in these classes hence this wrapper/decorator class has been
 * introduced. Use this decorator to add behaviour that would otherwise have been added to the
 * original class
 */
public class OffenceDecisionDecorator extends QueryOffenceDecision {

    public OffenceDecisionDecorator(final QueryOffenceDecision offenceDecision) {
        super(
                offenceDecision.getAdditionalPointsReason(),
                offenceDecision.getAdjournedTo(),
                offenceDecision.getBackDuty(),
                offenceDecision.getCompensation(),
                offenceDecision.getConvictionDate(),
                offenceDecision.getDecisionType(),
                offenceDecision.getDischargeType(),
                offenceDecision.getDischargedFor(),
                offenceDecision.getDisqualification(),
                offenceDecision.getDisqualificationPeriod(),
                offenceDecision.getDisqualificationType(),
                offenceDecision.getEstimatedHearingDuration(),
                offenceDecision.getExcisePenalty(),
                offenceDecision.getFine(),
                offenceDecision.getGuiltyPleaTakenIntoAccount(),
                offenceDecision.getLicenceEndorsement(),
                offenceDecision.getMagistratesCourt(),
                offenceDecision.getNoCompensationReason(),
                offenceDecision.getNotionalPenaltyPoints(),
                offenceDecision.getOffenceId(),
                offenceDecision.getPenaltyPointsImposed(),
                offenceDecision.getPenaltyPointsReason(),
                offenceDecision.getPressRestriction(),
                offenceDecision.getReason(),
                offenceDecision.getReferralReasonId(),
                offenceDecision.getReferredToCourt(),
                offenceDecision.getReferredToDateTime(),
                offenceDecision.getReferredToRoom(),
                offenceDecision.getVerdict(),
                offenceDecision.getWithdrawalReasonId()
        );
    }

    public boolean hasPointsDisqualification() {
        return isTrue(getDisqualification()) && getDisqualificationType() == DisqualificationType.POINTS;
    }
}
