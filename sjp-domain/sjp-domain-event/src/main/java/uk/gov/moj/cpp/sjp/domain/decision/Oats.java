package uk.gov.moj.cpp.sjp.domain.decision;

import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.OATS;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class Oats extends SingleOffenceDecision {


    public Oats(final UUID id, final OffenceDecisionInformation offenceDecisionInformation) {
        this(id, offenceDecisionInformation, null);
    }

    @JsonCreator
    public Oats(@JsonProperty("id") final UUID id,
                @JsonProperty("offenceDecisionInformation") final OffenceDecisionInformation offenceDecisionInformation,
                @JsonProperty("pressRestriction") final PressRestriction pressRestriction) {
        super(id, OATS, offenceDecisionInformation, pressRestriction);
    }

    @Override
    public SessionCourt getConvictingCourt() {
        return null;
    }

    @Override
    public void accept(final OffenceDecisionVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public LocalDate getConvictionDate() { return null; }

    @Override
    public boolean equals(Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
}
