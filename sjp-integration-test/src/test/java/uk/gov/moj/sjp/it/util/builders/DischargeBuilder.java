package uk.gov.moj.sjp.it.util.builders;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationPeriod;
import uk.gov.moj.cpp.sjp.domain.decision.disqualification.DisqualificationType;
import uk.gov.moj.cpp.sjp.domain.decision.endorsement.PenaltyPointsReason;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.math.BigDecimal;
import java.util.UUID;

public class DischargeBuilder {


    private UUID id;
    private OffenceDecisionInformation offenceDecisionInformation;
    private DischargeType dischargeType;
    private DischargePeriod dischargedFor;
    private BigDecimal compensation;
    private String noCompensationReason;
    private Boolean guiltyPleaTakenIntoAccount;
    private BigDecimal backDuty;
    private VerdictType verdict;
    private Boolean licenceEndorsed;
    private Integer penaltyPointsImposed;
    private PenaltyPointsReason penaltyPointsReason;
    private String additionalPointsReason;
    private Boolean disqualification;
    private DisqualificationType disqualificationType;
    private DisqualificationPeriod disqualificationPeriod;
    private Integer notionalPenaltyPoints;
    private PressRestriction pressRestriction;


    public static DischargeBuilder withDefaults() {
        final DischargeBuilder builder = new DischargeBuilder();
        builder.id = randomUUID();
        builder.dischargeType = ABSOLUTE;
        builder.verdict = PROVED_SJP;
        builder.dischargedFor = null;
        builder.compensation = BigDecimal.TEN;
        builder.noCompensationReason = null;
        builder.guiltyPleaTakenIntoAccount = false;
        builder.backDuty = null;
        builder.pressRestriction = null;
        builder.licenceEndorsed = null;
        builder.penaltyPointsImposed = null;
        builder.penaltyPointsReason = null;
        builder.additionalPointsReason = null;
        builder.disqualification = null;
        builder.disqualificationType = null;
        builder.disqualificationPeriod = null;
        builder.notionalPenaltyPoints = null;
        return builder;
    }

    public Discharge build() {
        this.offenceDecisionInformation = createOffenceDecisionInformation(this.id, this.verdict);
        return new Discharge(
                id,
                offenceDecisionInformation,
                dischargeType,
                dischargedFor,
                compensation,
                noCompensationReason,
                guiltyPleaTakenIntoAccount,
                backDuty,
                licenceEndorsed,
                penaltyPointsImposed,
                penaltyPointsReason,
                additionalPointsReason,
                disqualification,
                disqualificationType,
                disqualificationPeriod,
                notionalPenaltyPoints,
                pressRestriction
        );
    }

    public DischargeBuilder id(final UUID offenceId) {
        this.id = offenceId;
        return this;
    }

    public DischargeBuilder pressRestriction(final String name) {
        this.pressRestriction = new PressRestriction(name);
        return this;
    }

    public DischargeBuilder withVerdict(final VerdictType verdict) {
        this.verdict = verdict;
        return this;
    }

    public DischargeBuilder withDischargeType(final DischargeType dischargeType) {
        this.dischargeType = dischargeType;
        return this;
    }

    public DischargeBuilder withDischargedFor(final DischargePeriod dischargePeriod) {
        this.dischargedFor = dischargePeriod;
        return this;
    }

    public DischargeBuilder withCompensation(final BigDecimal compensation) {
        this.compensation = compensation;
        return this;
    }

    public DischargeBuilder withDisqualification(final DisqualificationType disqualificationType) {
        this.disqualification = true;
        this.disqualificationType = disqualificationType;
        return this;
    }
}
