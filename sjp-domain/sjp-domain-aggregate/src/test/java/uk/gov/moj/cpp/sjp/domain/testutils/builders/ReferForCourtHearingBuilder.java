package uk.gov.moj.cpp.sjp.domain.testutils.builders;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.NO_DISABILITY_NEEDS;

import uk.gov.justice.core.courts.NextHearing;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ReferForCourtHearingBuilder {

    private static final int TEN_MINUTES = 10;
    private static final DefendantCourtOptions ENGLISH_NO_INTERPRETER = new DefendantCourtOptions(
            new DefendantCourtInterpreter("EN", true), false, NO_DISABILITY_NEEDS);

    private UUID id;
    private PressRestriction pressRestriction;
    private List<OffenceDecisionInformation> offenceDecisionInformation;
    private UUID referralReasonId;
    private String listingNotes;
    private Integer estimatedHearingDuration;
    private DefendantCourtOptions defendantCourtOptions;
    private NextHearing nextHearing;

    public static ReferForCourtHearingBuilder withDefaults() {
        final ReferForCourtHearingBuilder builder = new ReferForCourtHearingBuilder();
        builder.id = randomUUID();
        builder.offenceDecisionInformation = new ArrayList<>();
        builder.referralReasonId = randomUUID();
        builder.listingNotes = "Listing notes";
        builder.estimatedHearingDuration = TEN_MINUTES;
        builder.defendantCourtOptions = ENGLISH_NO_INTERPRETER;
        return builder;
    }

    public ReferForCourtHearingBuilder pressRestriction(final String name) {
        this.pressRestriction = PressRestriction.requested(name);
        return this;
    }

    public ReferForCourtHearing build() {
        return new ReferForCourtHearing(
                id,
                offenceDecisionInformation,
                referralReasonId,
                null,
                listingNotes,
                estimatedHearingDuration,
                defendantCourtOptions,
                pressRestriction, nextHearing
        );
    }

    public ReferForCourtHearingBuilder addOffenceDecisionInformation(final VerdictType verdict) {
        this.offenceDecisionInformation.add(createOffenceDecisionInformation(randomUUID(), verdict));
        return this;
    }

    public ReferForCourtHearingBuilder addOffenceDecisionInformation(final UUID offenceId, final VerdictType verdict) {
        this.offenceDecisionInformation.add(createOffenceDecisionInformation(offenceId, verdict));
        return this;
    }

    public ReferForCourtHearingBuilder withNextHearing(final NextHearing nextHearing) {
        this.nextHearing = nextHearing;
        return this;
    }

}
