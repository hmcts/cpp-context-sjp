package uk.gov.moj.cpp.sjp.domain.testutils.builders;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.Discharge.createDischarge;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType.ABSOLUTE;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;

import uk.gov.moj.cpp.sjp.domain.decision.Discharge;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargePeriod;
import uk.gov.moj.cpp.sjp.domain.decision.discharge.DischargeType;

import java.math.BigDecimal;
import java.util.UUID;

public class DischargeBuilder {

    private UUID id;
    private OffenceDecisionInformation offenceDecisionInformation;
    private DischargeType dischargeType;
    private DischargePeriod dischargedFor;
    private BigDecimal compensation;
    private String noCompensationReason;
    private boolean guiltyPleaTakenIntoAccount;
    private BigDecimal backDuty;
    private PressRestriction pressRestriction;

    public static DischargeBuilder withDefaults() {
        final DischargeBuilder builder = new DischargeBuilder();
        builder.id = randomUUID();
        builder.dischargeType = ABSOLUTE;
        builder.dischargedFor = null;
        builder.compensation = BigDecimal.TEN;
        builder.noCompensationReason = null;
        builder.guiltyPleaTakenIntoAccount = false;
        builder.backDuty = null;
        builder.pressRestriction = null;
        return builder;
    }

    public Discharge build() {
        return createDischarge(
                this.id,
                this.offenceDecisionInformation = createOffenceDecisionInformation(this.id, PROVED_SJP),
                this.dischargeType,
                this.dischargedFor,
                this.compensation,
                this.noCompensationReason,
                this.guiltyPleaTakenIntoAccount,
                this.backDuty,
                this.pressRestriction
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
}
