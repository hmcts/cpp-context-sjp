package uk.gov.moj.sjp.it.model;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;
import static org.apache.commons.lang3.builder.HashCodeBuilder.reflectionHashCode;

import uk.gov.justice.json.schemas.domains.sjp.User;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.imposition.FinancialImposition;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class DecisionCommand {

    private UUID sessionId;
    private UUID caseId;
    private String note;
    private User savedBy;
    private ZonedDateTime savedAt;
    private List<? extends OffenceDecision> offenceDecisions;
    private FinancialImposition financialImposition;

    public DecisionCommand(final UUID sessionId,
                           final UUID caseId,
                           final String note,
                           final User savedBy,
                           final List<? extends OffenceDecision> offencesDecisions,
                           final FinancialImposition financialImposition) {
        this.sessionId = sessionId;
        this.caseId = caseId;
        this.note = note;
        this.savedBy = savedBy;
        this.offenceDecisions = offencesDecisions;
        this.financialImposition = financialImposition;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public String getNote() {
        return note;
    }

    public List<? extends OffenceDecision> getOffenceDecisions() {
        return offenceDecisions;
    }

    public FinancialImposition getFinancialImposition() {
        return financialImposition;
    }

    @JsonIgnore
    public UUID getCaseId() {
        return caseId;
    }

    @JsonIgnore
    public User getSavedBy() {
        return savedBy;
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
