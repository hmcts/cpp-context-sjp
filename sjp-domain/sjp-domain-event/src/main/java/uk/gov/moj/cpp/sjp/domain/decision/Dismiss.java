package uk.gov.moj.cpp.sjp.domain.decision;

import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.DISMISS;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Dismiss extends SingleOffenceDecision {

    public Dismiss(final UUID id, final OffenceDecisionInformation offenceDecisionInformation) {
        super(id, DISMISS, offenceDecisionInformation, null);
    }

    @JsonCreator
    public Dismiss(@JsonProperty("id") final UUID id,
                   @JsonProperty("offenceDecisionInformation") final OffenceDecisionInformation offenceDecisionInformation,
                   @JsonProperty("pressRestriction") final PressRestriction pressRestriction) {
        super(id, DISMISS, offenceDecisionInformation, pressRestriction);
    }

    @Override
    public void accept(final OffenceDecisionVisitor visitor) {
        visitor.visit(this);
    }

}
