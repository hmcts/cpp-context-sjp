package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.util.UUID;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("DISMISS")
public class DismissOffenceDecision extends OffenceDecision {

    public DismissOffenceDecision() {
        super();
    }

    public DismissOffenceDecision(final UUID offenceId, final UUID caseDecisionId, final VerdictType verdict) {
        super(offenceId, caseDecisionId, DecisionType.DISMISS, verdict);
    }
}
