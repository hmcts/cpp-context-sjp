package uk.gov.moj.cpp.sjp.persistence.entity;

import static com.google.common.collect.ImmutableList.copyOf;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

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
}
