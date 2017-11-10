package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "court_referral")
public class CourtReferral {

    @Id
    @Column(name="case_id")
    private UUID caseId;
    @Column(name="hearing_date")
    private LocalDate hearingDate;
    @Column(name="actioned")
    private ZonedDateTime actioned;

    private CourtReferral() {
    }

    public CourtReferral(final UUID caseId, final LocalDate hearingDate) {
        this.caseId = caseId;
        this.hearingDate = hearingDate;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public LocalDate getHearingDate() {
        return hearingDate;
    }

    public void setActioned(final ZonedDateTime actioned) {
        this.actioned = actioned;
    }

    public ZonedDateTime getActioned() {
        return actioned;
    }
}
