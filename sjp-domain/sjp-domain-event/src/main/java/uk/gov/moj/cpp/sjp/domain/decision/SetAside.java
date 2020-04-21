package uk.gov.moj.cpp.sjp.domain.decision;

import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.SET_ASIDE;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SetAside extends MultipleOffenceDecision {


    @JsonCreator
    public SetAside(@JsonProperty("id") final UUID id,
                    @JsonProperty("offenceDecisionInformation") final List<OffenceDecisionInformation> offenceDecisionInformation) {
        super(id, SET_ASIDE, offenceDecisionInformation);
    }

    @Override
    public void accept(final OffenceDecisionVisitor visitor) {
        visitor.visit(this);
    }

}
