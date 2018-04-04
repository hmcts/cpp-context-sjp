package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "pending_dates_to_avoid")
public class PendingDatesToAvoid {
    @Id
    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "plea_date")
    private ZonedDateTime pleaDate;

    @OneToOne
    @JoinColumn(name = "case_id", nullable = false)
    @JsonIgnore
    private CaseDetail caseDetail;

    public PendingDatesToAvoid() {}

    public PendingDatesToAvoid(final UUID caseId) {
        this.caseId = caseId;
    }

    public PendingDatesToAvoid(final UUID caseId, final ZonedDateTime pleaDate) {
        this.caseId = caseId;
        this.pleaDate = pleaDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public ZonedDateTime getPleaDate() {
        return pleaDate;
    }

    public void setPleaDate(final ZonedDateTime pleaDate) {
        this.pleaDate = pleaDate;
    }

    public CaseDetail getCaseDetail() {
        return caseDetail;
    }

    public void setCaseDetail(final CaseDetail caseDetail) {
        this.caseDetail = caseDetail;
    }
}
