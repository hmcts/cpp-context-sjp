package uk.gov.moj.cpp.sjp.domain.decision;

import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFERRED_FOR_FUTURE_SJP_SESSION;

import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ReferredForFutureSJPSession extends MultipleOffenceDecision {

    @JsonCreator
    public ReferredForFutureSJPSession(@JsonProperty("id") final UUID id,
                                       @JsonProperty("offenceDecisionInformation") final List<OffenceDecisionInformation> offenceDecisionInformation) {

        super(id, REFERRED_FOR_FUTURE_SJP_SESSION, offenceDecisionInformation);
    }

    @Override
    public void accept(final OffenceDecisionVisitor visitor) {
        visitor.visit(this);
    }
}
