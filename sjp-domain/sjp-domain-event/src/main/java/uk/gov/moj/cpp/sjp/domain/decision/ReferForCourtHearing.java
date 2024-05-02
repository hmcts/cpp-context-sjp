package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;

import uk.gov.justice.core.courts.NextHearing;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReferForCourtHearing extends MultipleOffenceDecision implements ConvictingDecision {

    private final UUID referralReasonId;
    private final String referralReason;
    private final String listingNotes;
    private final Integer estimatedHearingDuration;
    private final DefendantCourtOptions defendantCourtOptions;
    private LocalDate convictionDate;
    private SessionCourt convictingCourt;
    private final NextHearing nextHearing;

    public ReferForCourtHearing(final UUID id,
                                final List<OffenceDecisionInformation> offenceDecisionInformation,
                                final UUID referralReasonId,
                                final String listingNotes,
                                final Integer estimatedHearingDuration,
                                final DefendantCourtOptions defendantCourtOptions,
                                final NextHearing nextHearing) {
        this(id, offenceDecisionInformation, referralReasonId, null, listingNotes,
                estimatedHearingDuration, defendantCourtOptions, null, nextHearing);
    }

    @JsonCreator
    public ReferForCourtHearing(@JsonProperty("id") final UUID id,
                                @JsonProperty("offenceDecisionInformation") final List<OffenceDecisionInformation> offenceDecisionInformation,
                                @JsonProperty("referralReasonId") final UUID referralReasonId,
                                @JsonProperty("referralReason") final String referralReason,
                                @JsonProperty("listingNotes") final String listingNotes,
                                @JsonProperty("estimatedHearingDuration") final Integer estimatedHearingDuration,
                                @JsonProperty("defendantCourtOptions") final DefendantCourtOptions defendantCourtOptions,
                                @JsonProperty("pressRestriction") final PressRestriction pressRestriction,
                                @JsonProperty("nextHearing") final NextHearing nextHearing) {
        super(id, REFER_FOR_COURT_HEARING, offenceDecisionInformation, pressRestriction);
        this.referralReasonId = referralReasonId;
        this.referralReason = referralReason;
        this.listingNotes = listingNotes;
        this.estimatedHearingDuration = estimatedHearingDuration;
        this.defendantCourtOptions = defendantCourtOptions;
        this.nextHearing = nextHearing;
    }

    public UUID getReferralReasonId() {
        return referralReasonId;
    }

    public String getReferralReason() {
        return referralReason;
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

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public void setConvictionDate(final LocalDate convictionDate) {
        this.convictionDate = convictionDate;
    }

    public SessionCourt getConvictingCourt() {
        return convictingCourt;
    }

    public void setConvictingCourt(final SessionCourt convictingCourt) {
        this.convictingCourt = convictingCourt;
    }

    public NextHearing getNextHearing() {
        return nextHearing;
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
