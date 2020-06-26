package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.FINANCIAL_PENALTY;

import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FinancialPenalty extends DisqualifyEndorseDecision {

    private BigDecimal fine;

    private BigDecimal compensation;

    private String noCompensationReason;

    private Boolean guiltyPleaTakenIntoAccount;

    private BigDecimal backDuty;

    private BigDecimal excisePenalty;

    private LocalDate convictionDate;

    @SuppressWarnings("squid:S00107")
    public FinancialPenalty(@JsonProperty("id") final UUID id,
                            @JsonProperty("offenceDecisionInformation") final OffenceDecisionInformation offenceDecisionInformation,
                            @JsonProperty("fine") final BigDecimal fine,
                            @JsonProperty("compensation") final BigDecimal compensation,
                            @JsonProperty("noCompensationReason") final String noCompensationReason,
                            @JsonProperty("guiltyPleaTakenIntoAccount") final Boolean guiltyPleaTakenIntoAccount,
                            @JsonProperty("backDuty") final BigDecimal backDuty,
                            @JsonProperty("excisePenalty") final BigDecimal excisePenalty,
                            @JsonProperty("licenceEndorsed") final Boolean licenceEndorsed,
                            @JsonProperty("penaltyPointsImposed") final Integer penaltyPointsImposed,
                            @JsonProperty("penaltyPointsReason") final PenaltyPointsReason penaltyPointsReason,
                            @JsonProperty("additionalPointsReason") final String additionalPointsReason,
                            @JsonProperty("disqualification") final Boolean disqualification,
                            @JsonProperty("disqualificationType") final DisqualificationType disqualificationType,
                            @JsonProperty("disqualificationPeriod") final DisqualificationPeriod disqualificationPeriod,
                            @JsonProperty("notionalPenaltyPoints") final Integer notionalPenaltyPoints,
                            @JsonProperty("pressRestriction") final PressRestriction pressRestriction
                            ) {
        super(id, FINANCIAL_PENALTY, offenceDecisionInformation, licenceEndorsed,
                penaltyPointsImposed, penaltyPointsReason, additionalPointsReason,
                disqualification, disqualificationType, disqualificationPeriod, notionalPenaltyPoints,
                pressRestriction);
        this.compensation = compensation;
        this.fine = fine;
        this.noCompensationReason = noCompensationReason;
        this.guiltyPleaTakenIntoAccount = guiltyPleaTakenIntoAccount;
        this.backDuty = backDuty;
        this.excisePenalty = excisePenalty;
    }

    public static FinancialPenalty createFinancialPenalty(final UUID id,
                                                          final OffenceDecisionInformation offenceDecisionInformation,
                                                          final BigDecimal fine,
                                                          final BigDecimal compensation,
                                                          final String noCompensationReason,
                                                          final Boolean guiltyPleaTakenIntoAccount,
                                                          final BigDecimal backDuty,
                                                          final BigDecimal excisePenalty,
                                                          final PressRestriction pressRestriction) {
        return new FinancialPenalty(id, offenceDecisionInformation, fine, compensation, noCompensationReason, guiltyPleaTakenIntoAccount, backDuty, excisePenalty,
                null, null, null,null, null, null, null, null, pressRestriction);
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

    public BigDecimal getBackDuty() {
        return backDuty;
    }

    public BigDecimal getExcisePenalty() {
        return excisePenalty;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public void setConvictionDate(final LocalDate convictionDate) {
        this.convictionDate = convictionDate;
    }

    @Override
    public void accept(final OffenceDecisionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }

}
