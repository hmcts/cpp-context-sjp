package uk.gov.moj.cpp.sjp.persistence.entity;

import static java.time.LocalDate.now;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "case_publish_status")
public class CasePublishStatus implements Serializable {

    private static final long serialVersionUID = -1581766510152881247L;

    @Id
    @Column(name = "case_id", nullable = false, unique = true)
    private UUID caseId;

    @Column(name = "first_published")
    private LocalDate firstPublished;

    @Column(name = "number_of_publishes")
    private Integer numberOfPublishes = 0;

    @Column(name = "total_number_of_publishes")
    private Integer totalNumberOfPublishes = 0;

    public CasePublishStatus(final UUID caseId, final LocalDate firstPublished, final Integer numberOfPublishes, final Integer totalNumberOfPublishes) {
        this.caseId = caseId;
        this.firstPublished = firstPublished;
        this.numberOfPublishes = numberOfPublishes;
        this.totalNumberOfPublishes = totalNumberOfPublishes;
    }

    public CasePublishStatus() {
        //for JPA
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(final UUID caseId) {
        this.caseId = caseId;
    }

    public LocalDate getFirstPublished() {
        return firstPublished;
    }

    public void setFirstPublished(final LocalDate firstPublished) {
        this.firstPublished = firstPublished;
    }

    public Integer getNumberOfPublishes() {
        return numberOfPublishes;
    }

    public void setNumberOfPublishes(final Integer numberOfPublishes) {
        this.numberOfPublishes = numberOfPublishes;
    }

    public Integer getTotalNumberOfPublishes() {
        return totalNumberOfPublishes;
    }

    public void setTotalNumberOfPublishes(final Integer totalNumberOfPublishes) {
        this.totalNumberOfPublishes = totalNumberOfPublishes;
    }

    public static CasePublishStatus createFirstPublishedCasePublishStatus(final UUID caseId) {
        return new CasePublishStatus(caseId, now(), 0, 0);
    }

    public void incrementPublishedCounters() {
        this.numberOfPublishes++;
        this.totalNumberOfPublishes++;
    }
}
