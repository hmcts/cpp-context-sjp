package uk.gov.moj.cpp.sjp.persistence.entity;

import static org.apache.commons.lang3.BooleanUtils.isTrue;

import uk.gov.justice.services.common.jpa.converter.LocalDatePersistenceConverter;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "case_details")
public class CaseSummary implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id")
    private UUID id;
    @Column(name = "urn")
    private String urn;
    @Column(name = "enterprise_id")
    private String enterpriseId;
    @Column(name = "prosecuting_authority")
    private String prosecutingAuthority;
    @Column(name = "posting_date")
    @Convert(converter = LocalDatePersistenceConverter.class)
    private LocalDate postingDate;
    @Column(name = "reopened_date")
    private LocalDate reopenedDate;
    @Column(name = "completed")
    private Boolean completed = Boolean.FALSE;
    @Column(name = "listed_in_criminal_courts")
    private Boolean listedInCriminalCourts = Boolean.FALSE;

    @Column(name = "dates_to_avoid")
    private String datesToAvoid;

    @Column(name = "referred_for_court_hearing")
    private Boolean referredForCourtHearing;

    @Column(name = "adjourned_to")
    private LocalDate adjournedTo;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUrn() {
        return urn;
    }

    public void setUrn(String urn) {
        this.urn = urn;
    }

    public String getEnterpriseId() {
        return enterpriseId;
    }

    public void setEnterpriseId(String enterpriseId) {
        this.enterpriseId = enterpriseId;
    }

    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public void setProsecutingAuthority(String prosecutingAuthority) {
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(LocalDate postingDate) {
        this.postingDate = postingDate;
    }

    public LocalDate getReopenedDate() {
        return reopenedDate;
    }

    public void setReopenedDate(LocalDate reopenedDate) {
        this.reopenedDate = reopenedDate;
    }

    public Boolean isCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Boolean getListedInCriminalCourts() {
        return listedInCriminalCourts;
    }

    public void setListedInCriminalCourts(Boolean listedInCriminalCourts) {
        this.listedInCriminalCourts = listedInCriminalCourts;
    }

    public String getDatesToAvoid() {
        return datesToAvoid;
    }

    public void setDatesToAvoid(final String datesToAvoid) {
        this.datesToAvoid = datesToAvoid;
    }

    public Boolean getReferredForCourtHearing() {
        return referredForCourtHearing;
    }

    public void setReferredForCourtHearing(final Boolean referredForCourtHearing) {
        this.referredForCourtHearing = referredForCourtHearing;
    }

    public boolean isReferredForCourtHearing() {
        return isTrue(referredForCourtHearing);
    }

    public LocalDate getAdjournedTo() {
        return adjournedTo;
    }
}
