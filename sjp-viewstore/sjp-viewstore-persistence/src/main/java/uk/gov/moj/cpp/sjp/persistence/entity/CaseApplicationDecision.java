package uk.gov.moj.cpp.sjp.persistence.entity;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "application_decision")
public class CaseApplicationDecision implements Serializable {

    @Id
    @Column(name = "id")
    private UUID decisionId;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @OneToOne(optional = false)
    @JoinColumn(name = "application_id")
    private CaseApplication caseApplication;

    @ManyToOne(optional = false)
    @JoinColumn(name = "session_id")
    private Session session;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "out_of_time")
    private Boolean outOfTime;

    @Column(name = "out_of_time_reason")
    private String outOfTimeReason;

    @Column(name = "saved_at", nullable = false)
    private ZonedDateTime savedAt;

    public UUID getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(final UUID decisionId) {
        this.decisionId = decisionId;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(final boolean granted) {
        this.granted = granted;
    }

    public CaseApplication getCaseApplication() {
        return caseApplication;
    }

    public void setCaseApplication(final CaseApplication caseApplication) {
        this.caseApplication = caseApplication;
    }

    public Session getSession() {
        return session;
    }

    public void setSession(final Session session) {
        this.session = session;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(final String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Boolean getOutOfTime() {
        return outOfTime;
    }

    public void setOutOfTime(final Boolean outOfTime) {
        this.outOfTime = outOfTime;
    }

    public String getOutOfTimeReason() {
        return outOfTimeReason;
    }

    public void setOutOfTimeReason(final String outOfTimeReason) {
        this.outOfTimeReason = outOfTimeReason;
    }

    public ZonedDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(final ZonedDateTime savedAt) {
        this.savedAt = savedAt;
    }
}
