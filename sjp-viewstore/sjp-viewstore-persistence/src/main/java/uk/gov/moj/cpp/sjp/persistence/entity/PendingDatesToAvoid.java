package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

@Entity
@Table(name = "pending_dates_to_avoid")
public class PendingDatesToAvoid {

    @Id
    @Column(name = "case_id")
    private UUID caseId;

    @Column(name = "plea_date")
    private ZonedDateTime pleaDate;

    @PrimaryKeyJoinColumn
    @OneToOne(fetch = FetchType.LAZY)
    private CaseDetail caseDetail;

    public PendingDatesToAvoid() {
    }

    public PendingDatesToAvoid(final UUID caseId) {
        this();
        this.caseId = caseId;
    }

    public PendingDatesToAvoid(final UUID caseId, final ZonedDateTime pleaDate) {
        this(caseId);
        this.pleaDate = pleaDate;
    }

    public PendingDatesToAvoid(final CaseDetail caseDetail) {
        this(caseDetail.getId());
        this.caseDetail = caseDetail;
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
