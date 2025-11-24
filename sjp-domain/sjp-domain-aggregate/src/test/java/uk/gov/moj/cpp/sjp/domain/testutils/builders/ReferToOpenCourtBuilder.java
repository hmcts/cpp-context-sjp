package uk.gov.moj.cpp.sjp.domain.testutils.builders;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;

import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredToOpenCourt;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReferToOpenCourtBuilder {

    private UUID id;
    private PressRestriction pressRestriction;
    private List<OffenceDecisionInformation> offenceDecisionInformation;

    public static ReferToOpenCourtBuilder withDefaults() {
        final ReferToOpenCourtBuilder builder = new ReferToOpenCourtBuilder();
        builder.id = randomUUID();
        builder.offenceDecisionInformation = new ArrayList<>();
        return builder;
    }

    public ReferToOpenCourtBuilder pressRestriction(final String name) {
        this.pressRestriction = PressRestriction.requested(name);
        return this;
    }

    public ReferredToOpenCourt build() {
        return new ReferredToOpenCourt(
                this.id,
                this.offenceDecisionInformation,
                "Listing notes",
                10,
                null,
                null,
                null,
                pressRestriction);
    }

    public ReferToOpenCourtBuilder addOffenceDecisionInformation(final VerdictType verdict) {
        this.offenceDecisionInformation.add(createOffenceDecisionInformation(randomUUID(), verdict));
        return this;
    }

    public ReferToOpenCourtBuilder addOffenceDecisionInformation(final UUID offenceId, final VerdictType verdict) {
        this.offenceDecisionInformation.add(createOffenceDecisionInformation(offenceId, verdict));
        return this;
    }

}
