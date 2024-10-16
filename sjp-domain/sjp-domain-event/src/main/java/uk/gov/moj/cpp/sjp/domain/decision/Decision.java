package uk.gov.moj.cpp.sjp.domain.decision;

import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@SuppressWarnings({"squid:S00107", "pmd:BeanMembersShouldSerialize"})
public class Decision implements Serializable {

    private UUID decisionId;
    private UUID sessionId;
    private UUID caseId;
    private String note;
    private ZonedDateTime savedAt;
    private User savedBy;
    private List<OffenceDecision> offenceDecisions;
    private FinancialImposition financialImposition;
    private Defendant defendant;

    @JsonCreator
    public Decision(@JsonProperty("decisionId") final UUID decisionId,
                    @JsonProperty("sessionId") final UUID sessionId,
                    @JsonProperty("caseId") final UUID caseId,
                    @JsonProperty("note") final String note,
                    @JsonProperty("savedAt") final ZonedDateTime savedAt,
                    @JsonProperty("savedBy") final User savedBy,
                    @JsonProperty("offenceDecisions") final List<OffenceDecision> offenceDecisions,
                    @JsonProperty("financialImposition") final FinancialImposition financialImposition,
                    @JsonProperty("defendant") final Defendant defendant) {
        this.decisionId = decisionId;
        this.sessionId = sessionId;
        this.caseId = caseId;
        this.note = note;
        this.savedAt = savedAt;
        this.savedBy = savedBy;
        this.offenceDecisions = offenceDecisions;
        this.financialImposition = financialImposition;
        this.defendant = defendant;
    }

    @SuppressWarnings("pmd:NullAssignment")
    public Decision(final UUID decisionId,
                    final UUID sessionId,
                    final UUID caseId,
                    final String note,
                    final ZonedDateTime savedAt,
                    final User savedBy,
                    final List<OffenceDecision> offenceDecisions,
                    final Defendant defendant) {
        this.decisionId = decisionId;
        this.sessionId = sessionId;
        this.caseId = caseId;
        this.note = note;
        this.savedAt = savedAt;
        this.savedBy = savedBy;
        this.offenceDecisions = offenceDecisions;
        this.defendant = defendant;
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

    public String getNote() {
        return note;
    }

    public ZonedDateTime getSavedAt() {
        return savedAt;
    }

    public User getSavedBy() {
        return savedBy;
    }

    public List<OffenceDecision> getOffenceDecisions() {
        return offenceDecisions;
    }

    public FinancialImposition getFinancialImposition() {
        return financialImposition;
    }

    public Defendant getDefendant() {
        return defendant;
    }

    @JsonIgnore
    public Set<UUID> getOffenceIds() {
        return offenceDecisions.stream()
                .flatMap(offenceDecision -> offenceDecision.getOffenceIds().stream())
                .collect(toSet());
    }

    @Override
    public boolean equals(Object o) {
        return reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return reflectionHashCode(this);
    }
}
