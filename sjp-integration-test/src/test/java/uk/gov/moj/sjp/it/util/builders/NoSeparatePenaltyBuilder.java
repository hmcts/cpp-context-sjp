package uk.gov.moj.sjp.it.util.builders;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;

import uk.gov.moj.cpp.sjp.domain.decision.NoSeparatePenalty;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;

import java.util.UUID;

public class NoSeparatePenaltyBuilder {

    private UUID id;
    private PressRestriction pressRestriction;

    public static NoSeparatePenaltyBuilder withDefaults() {
        return withDefaults(randomUUID());
    }

    public static NoSeparatePenaltyBuilder withDefaults(final UUID offenceId) {
        final NoSeparatePenaltyBuilder builder = new NoSeparatePenaltyBuilder();
        builder.id = offenceId;
        builder.pressRestriction = null;
        return builder;
    }

    public NoSeparatePenaltyBuilder pressRestriction(final String name) {
        this.pressRestriction = new PressRestriction(name);
        return this;
    }

    public NoSeparatePenalty build() {
        return new NoSeparatePenalty(
                this.id,
                createOffenceDecisionInformation(this.id, FOUND_GUILTY),
                false,
                false,
                pressRestriction);
    }

    public NoSeparatePenaltyBuilder id(final UUID offenceId) {
        this.id = offenceId;
        return this;
    }
}
