package uk.gov.moj.cpp.sjp.domain.decision;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.ADJOURN;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Adjourn extends MultipleOffenceDecision {

    private final String reason;
    private final LocalDate adjournTo;
    private LocalDate convictionDate;

    @JsonCreator
    public Adjourn(@JsonProperty("id") final UUID id,
                   @JsonProperty("offenceDecisionInformation") final List<OffenceDecisionInformation> offenceDecisionInformation,
                   @JsonProperty("reason") final String reason,
                   @JsonProperty("adjournTo") final LocalDate adjournTo) {

        super(id, ADJOURN, offenceDecisionInformation);
        this.reason = reason;
        this.adjournTo = adjournTo;
    }

    public String getReason() {
        return reason;
    }

    public LocalDate getAdjournTo() {
        return adjournTo;
    }

    public LocalDate getConvictionDate() {
        return convictionDate;
    }

    public void setConvictionDate(final LocalDate convictionDate) {
        this.convictionDate = convictionDate;
    }

    @Override
    public boolean equals(Object o) {
        return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }

    @Override
    public void accept(final OffenceDecisionVisitor visitor) {
        visitor.visit(this);
    }
}
