package uk.gov.moj.cpp.sjp.persistence.entity;

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
    @Column(name = "initiation_code")
    private String initiationCode;
    @Column(name = "prosecuting_authority")
    private String prosecutingAuthority;
    @Column(name = "posting_date")
    @Convert(converter = LocalDatePersistenceConverter.class)
    private LocalDate postingDate;
    @Column(name = "reopened_date")
    private LocalDate reopenedDate;
    @Column(name = "completed")
    private Boolean completed = Boolean.FALSE;

    public CaseSummary() {
    }

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

    public String getInitiationCode() {
        return initiationCode;
    }

    public void setInitiationCode(String initiationCode) {
        this.initiationCode = initiationCode;
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
}
