package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFERRED_TO_OPEN_COURT;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReferredToOpenCourt extends MultipleOffenceDecision {

    private final String referredToCourt;
    private final Integer referredToRoom;
    private final ZonedDateTime referredToDateTime;
    private final String reason;
    private final String magistratesCourt;

    public ReferredToOpenCourt(final UUID id,
                               final List<OffenceDecisionInformation> offenceDecisionInformation,
                               final String referredToCourt,
                               final Integer referredToRoom,
                               final ZonedDateTime referredToDateTime,
                               final String reason,
                               final String magistratesCourt) {
        this(id, offenceDecisionInformation, referredToCourt, referredToRoom, referredToDateTime, reason, magistratesCourt, null);
    }

    @JsonCreator
    public ReferredToOpenCourt(@JsonProperty("id") final UUID id,
                               @JsonProperty("offenceDecisionInformation") final List<OffenceDecisionInformation> offenceDecisionInformation,
                               @JsonProperty("referredToCourt") final String referredToCourt,
                               @JsonProperty("referredToRoom") final Integer referredToRoom,
                               @JsonProperty("referredToDateTime") final ZonedDateTime referredToDateTime,
                               @JsonProperty("reason") final String reason,
                               @JsonProperty("magistratesCourt") final String magistratesCourt,
                               @JsonProperty("pressRestriction") final PressRestriction pressRestriction) {

        super(id, REFERRED_TO_OPEN_COURT, offenceDecisionInformation, pressRestriction);
        this.referredToCourt = referredToCourt;
        this.referredToRoom = referredToRoom;
        this.referredToDateTime = referredToDateTime;
        this.reason = reason;
        this.magistratesCourt = magistratesCourt;
    }

    public String getReferredToCourt() {
        return referredToCourt;
    }

    public Integer getReferredToRoom() {
        return referredToRoom;
    }

    public ZonedDateTime getReferredToDateTime() {
        return referredToDateTime;
    }

    public String getReason() {
        return reason;
    }

    public String getMagistratesCourt() {
        return magistratesCourt;
    }

    @Override
    public SessionCourt getConvictingCourt() {
        return null;
    }

    @Override
    public LocalDate getConvictionDate() { return null; }

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
