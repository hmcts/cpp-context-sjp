package uk.gov.moj.sjp.it.util.builders;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;

import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;

import java.util.UUID;

public class DismissBuilder {

    private UUID id;
    private PressRestriction pressRestriction;

    public static DismissBuilder withDefaults() {
        return withDefaults(randomUUID());
    }

    public static DismissBuilder withDefaults(final UUID offenceId) {
        final DismissBuilder builder = new DismissBuilder();
        builder.id = offenceId;
        builder.pressRestriction = null;
        return builder;
    }

    public DismissBuilder pressRestriction(final String name) {
        this.pressRestriction = new PressRestriction(name);
        return this;
    }

    public Dismiss build() {
        return new Dismiss(this.id, createOffenceDecisionInformation(this.id, FOUND_NOT_GUILTY), pressRestriction);
    }

    public DismissBuilder id(final UUID offenceId) {
        this.id = offenceId;
        return this;
    }

    public DismissBuilder pressRestrictionRevoked() {
        this.pressRestriction = PressRestriction.revoked();
        return this;
    }
}
