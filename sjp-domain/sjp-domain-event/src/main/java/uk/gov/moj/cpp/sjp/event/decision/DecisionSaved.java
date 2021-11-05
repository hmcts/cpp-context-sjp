package uk.gov.moj.cpp.sjp.event.decision;


import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;
import static org.apache.commons.lang3.builder.ToStringBuilder.reflectionToString;
import static uk.gov.moj.cpp.sjp.domain.decision.DecisionType.REFER_FOR_COURT_HEARING;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(DecisionSaved.EVENT_NAME)
public class DecisionSaved implements Serializable {

    private static final long serialVersionUID = 4538558993636530549L;

    public static final String EVENT_NAME = "sjp.events.decision-saved";

    private UUID decisionId;
    private UUID sessionId;
    private UUID caseId;
    private ZonedDateTime savedAt;
    private List<OffenceDecision> offenceDecisions;
    private FinancialImposition financialImposition;

    @JsonCreator
    public DecisionSaved(@JsonProperty("decisionId") final UUID decisionId,
                         @JsonProperty("sessionId") final UUID sessionId,
                         @JsonProperty("caseId") final UUID caseId,
                         @JsonProperty("savedAt") final ZonedDateTime savedAt,
                         @JsonProperty("offenceDecisions") final List<OffenceDecision> offenceDecisions,
                         @JsonProperty("financialImposition") final FinancialImposition financialImposition) {
        this.decisionId = decisionId;
        this.sessionId = sessionId;
        this.caseId = caseId;
        this.savedAt = savedAt;
        this.offenceDecisions = offenceDecisions;
        this.financialImposition = financialImposition;
    }

    public DecisionSaved(final UUID decisionId,
                         final UUID sessionId,
                         final UUID caseId,
                         final ZonedDateTime savedAt,
                         final List<OffenceDecision> offenceDecisions) {
        this.decisionId = decisionId;
        this.sessionId = sessionId;
        this.caseId = caseId;
        this.savedAt = savedAt;
        this.offenceDecisions = offenceDecisions;
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public ZonedDateTime getSavedAt() {
        return savedAt;
    }

    public List<OffenceDecision> getOffenceDecisions() {
        return offenceDecisions;
    }

    public FinancialImposition getFinancialImposition() {
        return financialImposition;
    }

    @JsonIgnore
    public Optional<ReferForCourtHearing> getReferForCourtHearing() {
        return offenceDecisions
                .stream()
                .filter(offenceDecision -> offenceDecision.getType().equals(REFER_FOR_COURT_HEARING))
                .map(offenceDecision -> (ReferForCourtHearing) offenceDecision)
                .findFirst();
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
    public String toString() {
        return reflectionToString(this);
    }
}
