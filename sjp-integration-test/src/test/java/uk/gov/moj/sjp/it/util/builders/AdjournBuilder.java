package uk.gov.moj.sjp.it.util.builders;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;

import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AdjournBuilder {

    private PressRestriction pressRestriction;
    private List<OffenceDecisionInformation> offenceDecisionInformation;
    private LocalDate adjournTo;

    public static AdjournBuilder withDefaults() {
        final AdjournBuilder builder = new AdjournBuilder();
        builder.offenceDecisionInformation = new ArrayList<>();
        builder.adjournTo = LocalDate.now().plusDays(7);
        return builder;
    }

    public AdjournBuilder pressRestriction(final String name) {
        this.pressRestriction = PressRestriction.requested(name);
        return this;
    }

    public Adjourn build() {
        return new Adjourn(
                randomUUID(),
                offenceDecisionInformation,
                "Random reason",
                adjournTo,
                pressRestriction
        );
    }

    public AdjournBuilder addOffenceDecisionInformation(final VerdictType verdict) {
        this.offenceDecisionInformation.add(createOffenceDecisionInformation(randomUUID(), verdict));
        return this;
    }
}
