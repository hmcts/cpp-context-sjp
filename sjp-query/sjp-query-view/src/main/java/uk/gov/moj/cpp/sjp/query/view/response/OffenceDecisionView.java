package uk.gov.moj.cpp.sjp.query.view.response;

import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;
import uk.gov.moj.cpp.sjp.persistence.entity.AdjournOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.DischargeOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialPenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.NoSeparatePenaltyOffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferForCourtHearingDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.ReferredToOpenCourtDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.WithdrawOffenceDecision;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class OffenceDecisionView {

    private LocalDate adjournedTo;
    private UUID offenceId;
    private DecisionType decisionType;
    private UUID withdrawalReasonId;
    private VerdictType verdict;
    private UUID referralReasonId;
    private Integer estimatedHearingDuration;
    private DischargeType dischargeType;
    private DischargePeriod dischargedFor;
    private BigDecimal compensation;
    private String noCompensationReason;
    private Boolean guiltyPleaTakenIntoAccount;
    private Boolean licenceEndorsement;
    private BigDecimal fine;
    private String magistratesCourt;
    private String reason;
    private String referredToCourt;
    private Integer referredToRoom;
    private ZonedDateTime referredToDateTime;
    private BigDecimal backDuty;
    private BigDecimal excisePenalty;
    private LocalDate convictionDate;
    private Integer penaltyPointsImposed;
    private PenaltyPointsReason penaltyPointsReason;
    private String additionalPointsReason;
    private Boolean disqualification;
    private DisqualificationType disqualificationType;
    private DisqualificationPeriodView disqualificationPeriod;
    private Integer notionalPenaltyPoints;


    public OffenceDecisionView(final OffenceDecision offenceDecision) {
        this.offenceId = offenceDecision.getOffenceId();
        this.decisionType = offenceDecision.getDecisionType();
        this.verdict = offenceDecision.getVerdictType();
        this.convictionDate = offenceDecision.getConvictionDate();
        switch (offenceDecision.getDecisionType()) {
            case WITHDRAW:
                this.withdrawalReasonId = ((WithdrawOffenceDecision) offenceDecision).getWithdrawalReasonId();
                break;
            case ADJOURN:
                this.adjournedTo = ((AdjournOffenceDecision) offenceDecision).getAdjournedTo();
                break;
            case REFER_FOR_COURT_HEARING:
                final ReferForCourtHearingDecision referForCourtHearingDecision = (ReferForCourtHearingDecision) offenceDecision;
                this.referralReasonId = referForCourtHearingDecision.getReferralReasonId();
                this.estimatedHearingDuration = referForCourtHearingDecision.getEstimatedHearingDuration();
                break;
            case DISCHARGE:
                final DischargeOffenceDecision discharge = (DischargeOffenceDecision) offenceDecision;
                setDischarge(discharge);
                break;
            case FINANCIAL_PENALTY:
                final FinancialPenaltyOffenceDecision financialPenalty = (FinancialPenaltyOffenceDecision) offenceDecision;
                setFinancialPenalty(financialPenalty);
                break;
            case REFERRED_TO_OPEN_COURT:
                final ReferredToOpenCourtDecision referredToOpenCourtDecision = (ReferredToOpenCourtDecision) offenceDecision;
                setReferredToOpenCourtDecision(referredToOpenCourtDecision);
                break;
            case NO_SEPARATE_PENALTY:
                final NoSeparatePenaltyOffenceDecision noSeparatePenalty = (NoSeparatePenaltyOffenceDecision) offenceDecision;
                this.guiltyPleaTakenIntoAccount = noSeparatePenalty.getGuiltyPleaTakenIntoAccount();
                this.licenceEndorsement = noSeparatePenalty.getLicenceEndorsement();
                break;
            default:
                break;
        }
    }

    private void setReferredToOpenCourtDecision(ReferredToOpenCourtDecision referredToOpenCourtDecision) {
        this.magistratesCourt = referredToOpenCourtDecision.getMagistratesCourt();
        this.reason = referredToOpenCourtDecision.getReason();
        this.referredToCourt = referredToOpenCourtDecision.getReferredToCourt();
        this.referredToDateTime = referredToOpenCourtDecision.getReferredToDateTime();
        this.referredToRoom = referredToOpenCourtDecision.getReferredToRoom();
    }

    private void setFinancialPenalty(final FinancialPenaltyOffenceDecision financialPenalty) {
        this.compensation = financialPenalty.getCompensation();
        this.noCompensationReason = financialPenalty.getNoCompensationReason();
        this.guiltyPleaTakenIntoAccount = financialPenalty.isGuiltyPleaTakenIntoAccount();
        this.fine = financialPenalty.getFine();
        this.backDuty = financialPenalty.getBackDuty();
        this.excisePenalty = financialPenalty.getExcisePenalty();
        this.licenceEndorsement = financialPenalty.getLicenceEndorsement();
        this.penaltyPointsImposed = financialPenalty.getPenaltyPointsImposed();
        this.penaltyPointsReason = financialPenalty.getPenaltyPointsReason();
        this.additionalPointsReason = financialPenalty.getAdditionalPointsReason();
        this.disqualification = financialPenalty.getDisqualification();
        this.disqualificationType = financialPenalty.getDisqualificationType();
        this.disqualificationPeriod = ofNullable(financialPenalty.getDisqualificationPeriodValue())
                .map(disqualificationPeriodValue ->  new DisqualificationPeriodView(disqualificationPeriodValue, financialPenalty.getDisqualificationPeriodUnit()))
                .orElse(null);
        this.notionalPenaltyPoints = financialPenalty.getNotionalPenaltyPoints();
    }

    private void setDischarge(final DischargeOffenceDecision discharge) {
        this.dischargeType = discharge.getDischargeType();
        ofNullable(discharge.getDischargePeriod()).ifPresent(period ->
                this.dischargedFor = new DischargePeriod(period.getValue(), period.getUnit())
        );
        this.compensation = discharge.getCompensation();
        this.noCompensationReason = discharge.getNoCompensationReason();
        this.guiltyPleaTakenIntoAccount = discharge.isGuiltyPleaTakenIntoAccount();
        this.backDuty = discharge.getBackDuty();
        this.licenceEndorsement = discharge.getLicenceEndorsement();
        this.penaltyPointsImposed = discharge.getPenaltyPointsImposed();
        this.penaltyPointsReason = discharge.getPenaltyPointsReason();
        this.additionalPointsReason = discharge.getAdditionalPointsReason();
        this.disqualification = discharge.getDisqualification();
        this.disqualificationType = discharge.getDisqualificationType();
        this.disqualificationPeriod = ofNullable(discharge.getDisqualificationPeriodValue())
                .map(disqualificationPeriodValue ->  new DisqualificationPeriodView(disqualificationPeriodValue, discharge.getDisqualificationPeriodUnit()))
                .orElse(null);
        this.notionalPenaltyPoints = discharge.getNotionalPenaltyPoints();
    }

    public UUID getOffenceId() {
        return offenceId;
    }

    public DecisionType getDecisionType() {
        return decisionType;
    }

    public UUID getWithdrawalReasonId() {
        return withdrawalReasonId;
    }

    public VerdictType getVerdict() {
        return verdict;
    }

    public LocalDate getAdjournedTo() {
        return adjournedTo;
    }

    public UUID getReferralReasonId() {
        return referralReasonId;
    }

    public Integer getEstimatedHearingDuration() {
        return estimatedHearingDuration;
    }

    public DischargeType getDischargeType() {
        return dischargeType;
    }

    public DischargePeriod getDischargedFor() {
        return dischargedFor;
    }

    public BigDecimal getCompensation() {
        return compensation;
    }

    public String getNoCompensationReason() {
        return noCompensationReason;
    }

    public Boolean getGuiltyPleaTakenIntoAccount() {
        return guiltyPleaTakenIntoAccount;
    }

    public BigDecimal getFine() {
        return fine;
    }

    public String getMagistratesCourt() {
        return magistratesCourt;
    }

    public String getReason() {
        return reason;
    }

    public String getReferredToCourt() {
        return referredToCourt;
    }

    public ZonedDateTime getReferredToDateTime() {
        return referredToDateTime;
    }

    public Integer getReferredToRoom() {
        return referredToRoom;
    }

    public BigDecimal getBackDuty() {
        return backDuty;
    }

    public BigDecimal getExcisePenalty() {
        return excisePenalty;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public Boolean getLicenceEndorsement() {
        return licenceEndorsement;
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

    public DisqualificationPeriodView getDisqualificationPeriod() {
        return disqualificationPeriod;
    }

    public Integer getNotionalPenaltyPoints() {
        return notionalPenaltyPoints;
    }
}
