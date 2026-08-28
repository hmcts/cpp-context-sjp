package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.util.UUID;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("OATS")
public class OatsOffenceDecision extends OffenceDecision {

    public OatsOffenceDecision() { }

    public OatsOffenceDecision(final UUID offenceId,
                               final UUID caseDecisionId,
                               final VerdictType verdict,
                               final PressRestriction pressRestriction) {
        super(offenceId, caseDecisionId, DecisionType.OATS, verdict, null, pressRestriction);
    }
}
