package uk.gov.moj.cpp.sjp.domain.decision;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;

public class Dismiss extends SingleOffenceDecision {

    @JsonCreator
    public Dismiss(@JsonProperty("id") final UUID id,
                   @JsonProperty("offenceDecisionInformation") final OffenceDecisionInformation offenceDecisionInformation) {
        super(id, DISMISS, offenceDecisionInformation);
    }

    @Override
    public void accept(final OffenceDecisionVisitor visitor) {
        visitor.visit(this);
    }

}
