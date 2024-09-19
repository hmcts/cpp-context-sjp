package uk.gov.moj.cpp.sjp.event.decision;


import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings("squid:S107")
@Event(DecisionSaved.EVENT_NAME)
public class DecisionSaved implements Serializable {

    private static final long serialVersionUID = 7438255714694047836L;

    public static final String EVENT_NAME = "sjp.events.decision-saved";

    private UUID decisionId;
    private UUID sessionId;
    private UUID caseId;
    private String urn;
    private ZonedDateTime savedAt;
    private List<OffenceDecision> offenceDecisions;
    private FinancialImposition financialImposition;
    private Boolean resultedThroughAOCP;
    private String defendantName;
    private UUID defendantId;

    @JsonCreator
    public DecisionSaved(@JsonProperty("decisionId") final UUID decisionId,
                         @JsonProperty("sessionId") final UUID sessionId,
                         @JsonProperty("caseId") final UUID caseId,
                         @JsonProperty("urn") final String urn,
                         @JsonProperty("savedAt") final ZonedDateTime savedAt,
                         @JsonProperty("offenceDecisions") final List<OffenceDecision> offenceDecisions,
                         @JsonProperty("financialImposition") final FinancialImposition financialImposition,
                         @JsonProperty("defendantId") final UUID defendantId,
                         @JsonProperty("defendantName") final String defendantName,
                         @JsonProperty("resultedThroughAOCP") final Boolean resultedThroughAOCP) {
        this.decisionId = decisionId;
        this.sessionId = sessionId;
        this.caseId = caseId;
        this.urn = urn;
        this.savedAt = savedAt;
        if (nonNull(offenceDecisions)) {
            this.offenceDecisions = new ArrayList<>(offenceDecisions);
        }
        this.financialImposition = financialImposition;
        this.defendantId = defendantId;
        this.defendantName = defendantName;
        this.resultedThroughAOCP = resultedThroughAOCP;
    }

    public DecisionSaved(final UUID decisionId,
                         final UUID sessionId,
                         final UUID caseId,
                         final String urn,
                         final ZonedDateTime savedAt,
                         final List<OffenceDecision> offenceDecisions) {
        this.decisionId = decisionId;
        this.sessionId = sessionId;
        this.caseId = caseId;
        this.urn = urn;
        this.savedAt = savedAt;
        if (nonNull(offenceDecisions)) {
            this.offenceDecisions = new ArrayList<>(offenceDecisions);
        }
    }

    public DecisionSaved(final UUID decisionId,
                         final UUID sessionId,
                         final UUID caseId,
                         final String urn,
                         final ZonedDateTime savedAt,
                         final List<OffenceDecision> offenceDecisions,
                         final UUID defendantId,
                         final String defendantName) {
        this.decisionId = decisionId;
        this.sessionId = sessionId;
        this.caseId = caseId;
        this.urn = urn;
        this.savedAt = savedAt;
        if (nonNull(offenceDecisions)) {
            this.offenceDecisions = new ArrayList<>(offenceDecisions);
        }
        this.defendantId = defendantId;
        this.defendantName = defendantName;
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
        return unmodifiableList(offenceDecisions);
    }

    public FinancialImposition getFinancialImposition() {
        return financialImposition;
    }

    public Boolean getResultedThroughAOCP() {
        return resultedThroughAOCP;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public UUID getDefendantId() {
        return defendantId;
    }

    public String getUrn() {
        return urn;
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
