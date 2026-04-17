package uk.gov.moj.cpp.sjp.persistence.entity;

import static com.google.common.collect.ImmutableList.copyOf;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "case_decision")
public class CaseDecision implements Serializable {

    private static final long serialVersionUID = -252959198824297763L;

    @Column(name = "id")
    @Id
    private UUID id;

    @Column(name = "case_id")
    private UUID caseId;

    @ManyToOne
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(name = "saved_at")
    private ZonedDateTime savedAt;

    @Column(name = "resulted_through_aocp")
    private Boolean resultedThroughAOCP;

    @SuppressWarnings("squid:S1948")
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "caseDecisionId")
    private List<OffenceDecision> offenceDecisions = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "caseDecision")
    private FinancialImposition financialImposition;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(Session session) {
        this.session = session;
    }

    public ZonedDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(final ZonedDateTime savedAt) {
        this.savedAt = savedAt;
    }

    public List<OffenceDecision> getOffenceDecisions() {
        return copyOf(offenceDecisions);
    }

    public void setOffenceDecisions(List<OffenceDecision> offenceDecisions) {
        this.offenceDecisions = copyOf(offenceDecisions);
    }

    public FinancialImposition getFinancialImposition() {
        return financialImposition;
    }

    public void setFinancialImposition(FinancialImposition financialImposition) {
        this.financialImposition = financialImposition;
    }

    public Boolean getResultedThroughAOCP() {
        return resultedThroughAOCP;
    }

    public void setResultedThroughAOCP(final Boolean resultedThroughAOCP) {
        this.resultedThroughAOCP = resultedThroughAOCP;
    }
}
