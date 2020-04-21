package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.decision.DecisionType;
import uk.gov.moj.cpp.sjp.domain.verdict.VerdictType;

import java.util.UUID;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("REFERRED_FOR_FUTURE_SJP_SESSION")
public class ReferredForFutureSJPSessionDecision extends OffenceDecision {

    public ReferredForFutureSJPSessionDecision() { }

    public ReferredForFutureSJPSessionDecision(final UUID offenceId,
                                               final UUID caseDecisionId,
                                               final VerdictType verdict) {
        super(offenceId, caseDecisionId, DecisionType.REFERRED_FOR_FUTURE_SJP_SESSION, verdict, null);
    }
}
