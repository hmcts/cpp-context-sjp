package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.SET_ASIDE;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SetAside extends MultipleOffenceDecision {

    public SetAside(final UUID id,
                    final List<OffenceDecisionInformation> offenceDecisionInformation) {
        this(id, offenceDecisionInformation, null);
    }

    @JsonCreator
    public SetAside(@JsonProperty("id") final UUID id,
                    @JsonProperty("offenceDecisionInformation") final List<OffenceDecisionInformation> offenceDecisionInformation,
                    @JsonProperty("pressRestriction") final PressRestriction pressRestriction) {
        super(id, SET_ASIDE, offenceDecisionInformation, pressRestriction);
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
