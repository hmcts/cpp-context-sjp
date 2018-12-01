package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.List;
import java.util.UUID;

public class DefendantRequestView {

    private final UUID prosecutionCaseId;
    private final ReferralReasonView referralReason;
    private final String datesToAvoid;
    private final String summonsRequired;
    private final List<UUID> defendantOffences;

    public DefendantRequestView(final UUID prosecutionCaseId,
                                final ReferralReasonView referralReason,
                                final String datesToAvoid,
                                final String summonsRequired,
                                final List<UUID> defendantOffences) {

        this.prosecutionCaseId = prosecutionCaseId;
        this.referralReason = referralReason;
        this.datesToAvoid = datesToAvoid;
        this.summonsRequired = summonsRequired;
        this.defendantOffences = defendantOffences;
    }

    public UUID getProsecutionCaseId() {
        return prosecutionCaseId;
    }

    public ReferralReasonView getReferralReason() {
        return referralReason;
    }

    public String getDatesToAvoid() {
        return datesToAvoid;
    }

    public String getSummonsRequired() {
        return summonsRequired;
    }

    public List<UUID> getDefendantOffences() {
        return defendantOffences;
    }

}
