package uk.gov.moj.cpp.sjp.persistence.entity;


import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;

import java.util.UUID;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(DecisionType.DecisionName.SET_ASIDE)
public class SetAsideOffenceDecision extends OffenceDecision {

    public SetAsideOffenceDecision() {
        super();
    }

    public SetAsideOffenceDecision(final UUID offenceId,
                                   final UUID caseDecisionId,
                                   final PressRestriction pressRestriction) {
        super(offenceId, caseDecisionId, DecisionType.SET_ASIDE, null, null, pressRestriction);
    }
}
