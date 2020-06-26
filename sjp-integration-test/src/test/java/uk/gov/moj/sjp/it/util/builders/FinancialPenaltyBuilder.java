package uk.gov.moj.sjp.it.util.builders;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriodTimeUnit;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.math.BigDecimal;
import java.util.UUID;

public class FinancialPenaltyBuilder {
    private UUID id;
    private OffenceDecisionInformation offenceDecisionInformation;
    private BigDecimal fine;
    private BigDecimal compensation;
    private String noCompensationReason;
    private Boolean guiltyPleaTakenIntoAccount;
    private BigDecimal backDuty;
    private BigDecimal excisePenalty;
    private Boolean licenceEndorsed;
    private Integer penaltyPointsImposed;
    private PenaltyPointsReason penaltyPointsReason;
    private String additionalPointsReason;
    private Boolean disqualification;
    private DisqualificationType disqualificationType;
    private DisqualificationPeriod disqualificationPeriod;
    private Integer notionalPenaltyPoints;
    private PressRestriction pressRestriction;

    public static FinancialPenaltyBuilder withDefaults() {
        final FinancialPenaltyBuilder builder = new FinancialPenaltyBuilder();
        builder.id = randomUUID();
        builder.offenceDecisionInformation = createOffenceDecisionInformation(builder.id, PROVED_SJP);
        builder.fine = BigDecimal.TEN;
        builder.compensation = BigDecimal.ZERO;
        builder.noCompensationReason = "No compensation needed";
        builder.guiltyPleaTakenIntoAccount = false;
        builder.backDuty = null;
        builder.excisePenalty = null;
        builder.licenceEndorsed = false;
        builder.penaltyPointsImposed = null;
        builder.penaltyPointsReason = null;
        builder.additionalPointsReason = null;
        builder.disqualification = false;
        builder.disqualificationType = null;
        builder.disqualificationPeriod = null;
        builder.notionalPenaltyPoints = null;
        builder.pressRestriction = null;
        return builder;
    }

    public FinancialPenaltyBuilder id(final UUID id) {
        this.id = id;
        return this;
    }

    public FinancialPenaltyBuilder verdict(final VerdictType verdict) {
        this.offenceDecisionInformation = createOffenceDecisionInformation(this.id, verdict);
        return this;
    }

    public FinancialPenaltyBuilder licenceEndorsed(final boolean licenceEndorsement) {
        this.licenceEndorsed = licenceEndorsement;
        return this;
    }

    public FinancialPenaltyBuilder penaltyPointsImposed(final int points) {
        this.penaltyPointsImposed = points;
        this.penaltyPointsReason = null;
        return this;
    }

    public FinancialPenaltyBuilder penaltyPointsImposedWithReason(final int points, final PenaltyPointsReason reason) {
        this.penaltyPointsImposed = points;
        this.penaltyPointsReason = reason;
        return this;
    }

    public FinancialPenaltyBuilder disqualification(final boolean disqualification) {
        this.disqualification = disqualification;
        return this;
    }

    public FinancialPenaltyBuilder disqualificationType(final DisqualificationType disqualificationType) {
        this.disqualification = true;
        this.disqualificationType = disqualificationType;
        return this;
    }

    public FinancialPenaltyBuilder pressRestriction(final String name) {
        this.pressRestriction = new PressRestriction(name);
        return this;
    }

    public FinancialPenalty build() {
        return new FinancialPenalty(
                this.id,
                this.offenceDecisionInformation = createOffenceDecisionInformation(this.id, PROVED_SJP),
                this.fine,
                this.compensation,
                this.noCompensationReason,
                this.guiltyPleaTakenIntoAccount,
                this.backDuty,
                this.excisePenalty,
                this.licenceEndorsed,
                this.penaltyPointsImposed,
                this.penaltyPointsReason,
                this.additionalPointsReason,
                this.disqualification,
                this.disqualificationType,
                this.disqualificationPeriod,
                this.notionalPenaltyPoints,
                this.pressRestriction
        );
    }

    public FinancialPenaltyBuilder disqualificationPeriodInMonths(final int period) {
        this.disqualificationPeriod = new DisqualificationPeriod(period, DisqualificationPeriodTimeUnit.MONTH);
        return this;
    }
}
