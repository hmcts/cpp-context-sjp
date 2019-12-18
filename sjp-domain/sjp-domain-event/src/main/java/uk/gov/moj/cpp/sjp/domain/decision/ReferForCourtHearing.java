package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;

import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReferForCourtHearing extends MultipleOffenceDecision {

    private final UUID referralReasonId;
    private final String listingNotes;
    private final Integer estimatedHearingDuration;
    private final DefendantCourtOptions defendantCourtOptions;

    @JsonCreator
    public ReferForCourtHearing(@JsonProperty("id") final UUID id,
                                @JsonProperty("offenceDecisionInformation") final List<OffenceDecisionInformation> offenceDecisionInformation,
                                @JsonProperty("referralReasonId") final UUID referralReasonId,
                                @JsonProperty("listingNotes") final String listingNotes,
                                @JsonProperty("estimatedHearingDuration") final Integer estimatedHearingDuration,
                                @JsonProperty("defendantCourtOptions") final DefendantCourtOptions defendantCourtOptions) {

        super(id, REFER_FOR_COURT_HEARING, offenceDecisionInformation);
        this.referralReasonId = referralReasonId;
        this.listingNotes = listingNotes;
        this.estimatedHearingDuration = estimatedHearingDuration;
        this.defendantCourtOptions = defendantCourtOptions;
    }

    public UUID getReferralReasonId() {
        return referralReasonId;
    }

    public String getListingNotes() {
        return listingNotes;
    }

    public Integer getEstimatedHearingDuration() {
        return estimatedHearingDuration;
    }

    public DefendantCourtOptions getDefendantCourtOptions() {
        return defendantCourtOptions;
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
