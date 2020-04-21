package uk.gov.moj.cpp.sjp.domain.decision;

import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason;

import java.util.Objects;
import java.util.UUID;

public abstract class DisqualifyEndorseDecision extends SingleOffenceDecision {

    private Boolean licenceEndorsed;

    private Integer penaltyPointsImposed;

    private PenaltyPointsReason penaltyPointsReason;

    private String additionalPointsReason;

    private Boolean disqualification;

    private DisqualificationType disqualificationType;

    private DisqualificationPeriod disqualificationPeriod;

    private Integer notionalPenaltyPoints;

    public DisqualifyEndorseDecision(final UUID id, final DecisionType type, final OffenceDecisionInformation offenceDecisionInformation,
                                     final Boolean licenceEndorsed, final Integer penaltyPointsImposed,
                                     final PenaltyPointsReason penaltyPointsReason, final String additionalPointsReason,
                                     final Boolean disqualification, final DisqualificationType disqualificationType,
                                     DisqualificationPeriod disqualificationPeriod, final Integer notionalPenaltyPoints) {
        super(id, type, offenceDecisionInformation);
        this.licenceEndorsed = licenceEndorsed;
        this.penaltyPointsImposed = penaltyPointsImposed;
        this.penaltyPointsReason = penaltyPointsReason;
        this.additionalPointsReason = additionalPointsReason;
        this.disqualification = disqualification;
        this.disqualificationType = disqualificationType;
        this.disqualificationPeriod = disqualificationPeriod;
        this.notionalPenaltyPoints = notionalPenaltyPoints;
    }

    public Boolean getLicenceEndorsed() {
        return licenceEndorsed;
    }

    public Integer getPenaltyPointsImposed() {
        return penaltyPointsImposed;
    }

    public PenaltyPointsReason getPenaltyPointsReason() {
        return penaltyPointsReason;
    }

    public String getAdditionalPointsReason() {
        return additionalPointsReason;
    }

    public Boolean getDisqualification() {
        return disqualification;
    }

    public DisqualificationType getDisqualificationType() {
        return disqualificationType;
    }

    public DisqualificationPeriod getDisqualificationPeriod() {
        return disqualificationPeriod;
    }

    public Integer getNotionalPenaltyPoints() {
        return notionalPenaltyPoints;
    }

    @Override
    @SuppressWarnings("squid:S1067")
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DisqualifyEndorseDecision)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final DisqualifyEndorseDecision that = (DisqualifyEndorseDecision) o;
        return Objects.equals(licenceEndorsed, that.licenceEndorsed) &&
                Objects.equals(penaltyPointsImposed, that.penaltyPointsImposed) &&
                penaltyPointsReason == that.penaltyPointsReason &&
                Objects.equals(additionalPointsReason, that.additionalPointsReason) &&
                Objects.equals(disqualification, that.disqualification) &&
                disqualificationType == that.disqualificationType &&
                Objects.equals(disqualificationPeriod, that.disqualificationPeriod) &&
                Objects.equals(notionalPenaltyPoints, that.notionalPenaltyPoints);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), licenceEndorsed, penaltyPointsImposed, penaltyPointsReason, additionalPointsReason, disqualification, disqualificationType, disqualificationPeriod, notionalPenaltyPoints);
    }
}
