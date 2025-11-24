package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("REFERRED_TO_OPEN_COURT")
public class ReferredToOpenCourtDecision extends OffenceDecision {

    @Column(name = "referred_to_court")
    private String referredToCourt;

    @Column(name = "referred_to_room")
    private Integer referredToRoom;

    @Column(name = "referred_to_date_time")
    private ZonedDateTime referredToDateTime;

    @Column(name = "reason")
    private String reason;

    @Column(name = "magistrates_court")
    private String magistratesCourt;

    public ReferredToOpenCourtDecision() { }

    @SuppressWarnings("squid:S00107")
    public ReferredToOpenCourtDecision(final UUID offenceId,
                                       final UUID caseDecisionId,
                                       final VerdictType verdict,
                                       final String referredToCourt,
                                       final Integer referredToRoom,
                                       final ZonedDateTime referredToDateTime,
                                       final String reason,
                                       final String magistratesCourt,
                                       final PressRestriction pressRestriction) {
        super(offenceId, caseDecisionId, DecisionType.REFERRED_TO_OPEN_COURT, verdict, null, pressRestriction);
        this.referredToCourt = referredToCourt;
        this.referredToRoom = referredToRoom;
        this.referredToDateTime = referredToDateTime;
        this.reason = reason;
        this.magistratesCourt = magistratesCourt;
    }

    public String getReferredToCourt() {
        return referredToCourt;
    }

    public Integer getReferredToRoom() {
        return referredToRoom;
    }

    public ZonedDateTime getReferredToDateTime() {
        return referredToDateTime;
    }

    public String getReason() {
        return reason;
    }

    public String getMagistratesCourt() {
        return magistratesCourt;
    }

}
