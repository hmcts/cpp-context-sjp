package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(DecisionType.DecisionName.REFER_FOR_COURT_HEARING)
public class ReferForCourtHearingDecision extends OffenceDecision {

    @Column(name = "referral_reason_id")
    private UUID referralReasonId;

    @Column(name = "estimated_hearing_duration")
    private Integer estimatedHearingDuration;

    @Column(name = "listing_notes")
    private String listingNotes;

    public ReferForCourtHearingDecision() {
        super();
    }

    public ReferForCourtHearingDecision(final UUID offenceId, final UUID caseDecisionId,
                                        final UUID referralReasonId, final Integer estimatedHearingDuration,
                                        final String listingNotes,
                                        final VerdictType verdict,
                                        final LocalDate convictionDate,
                                        final PressRestriction pressRestriction) {

        super(offenceId, caseDecisionId, DecisionType.REFER_FOR_COURT_HEARING, verdict, convictionDate, pressRestriction);
        this.referralReasonId = referralReasonId;
        this.estimatedHearingDuration = estimatedHearingDuration;
        this.listingNotes = listingNotes;
    }

    public UUID getReferralReasonId() {
        return referralReasonId;
    }

    public Integer getEstimatedHearingDuration() {
        return estimatedHearingDuration;
    }

    public String getListingNotes() {
        return listingNotes;
    }
}
