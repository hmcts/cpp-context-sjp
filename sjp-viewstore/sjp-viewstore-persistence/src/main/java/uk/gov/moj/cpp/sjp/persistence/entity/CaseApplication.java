package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.justice.services.common.jpa.converter.LocalDatePersistenceConverter;
import uk.gov.moj.cpp.sjp.persistence.converter.JpaConverterJson;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.UUID;

import javax.json.JsonObject;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

@Entity
@Table(name = "case_application")
public class CaseApplication implements Serializable {

    @Id
    @Column(name = "id", unique = true, nullable = false)
    private UUID applicationId;

    @Column(name = "parent_application_id", nullable = true)
    private UUID parentApplicationId;

    @Column(name = "application_reference", nullable = false)
    private String applicationReference;

    @Column(name = "application_status")
    @Enumerated(EnumType.STRING)
    private ApplicationStatus applicationStatus;

    @OneToOne(mappedBy = "caseApplication")
    private CaseApplicationDecision applicationDecision;

    @Column(name = "application_code")
    private String typeCode;

    @Column(name = "application_type_id", nullable = false)
    private UUID typeId;

    @Column(name = "application_type")
    @Enumerated(EnumType.STRING)
    private ApplicationType applicationType;

    @ManyToOne(optional = false)
    @JoinColumn(name = "case_id", nullable = false)
    private CaseDetail caseDetail;

    @Column(name = "date_received", nullable = false)
    @Convert(converter = LocalDatePersistenceConverter.class)
    private LocalDate dateReceived;

    @Column(name = "out_of_time_reason", nullable = false)
    private String outOfTimeReason;

    @Column(name = "out_of_time", nullable = false)
    private boolean outOfTime;

    @Column(name = "initiated_application")
    @Convert(converter = JpaConverterJson.class)
    @SuppressWarnings("squid:S1948")
    private JsonObject initiatedApplication;

    public CaseApplication() {
    }

    public CaseApplication(final UUID id,
                           final UUID parentApplicationId,
                           final String applicationReference,
                           final ApplicationStatus ApplicationStatus,
                           final String typeCode,
                           final UUID typeId,
                           final ApplicationType applicationType,
                           final CaseDetail caseDetail,
                           final LocalDate dateReceived,
                           final String outOfTimeReason,
                           final Boolean outOfTime,
                           final JsonObject initiatedApplication) {
        this.applicationId = id;
        this.parentApplicationId = parentApplicationId;
        this.applicationReference = applicationReference;
        this.applicationStatus = ApplicationStatus;
        this.typeId = typeId;
        this.typeCode = typeCode;
        this.applicationType = applicationType;
        this.caseDetail = caseDetail;
        this.dateReceived = dateReceived;
        this.outOfTimeReason = outOfTimeReason;
        this.outOfTime = outOfTime;
        this.initiatedApplication = initiatedApplication;
    }


    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID id) {
        this.applicationId = id;
    }

    public UUID getParentApplicationId() {
        return parentApplicationId;
    }

    public void setParentApplicationId(UUID parentApplicationId) {
        this.parentApplicationId = parentApplicationId;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public void setApplicationReference(String applicationReference) {
        this.applicationReference = applicationReference;
    }

    public ApplicationStatus getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(ApplicationStatus applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public CaseApplicationDecision getApplicationDecision() {
        return applicationDecision;
    }

    public void setApplicationDecision(final CaseApplicationDecision applicationDecision) {
        this.applicationDecision = applicationDecision;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public UUID getTypeId() {
        return typeId;
    }

    public void setTypeId(UUID typeId) {
        this.typeId = typeId;
    }

    public ApplicationType getApplicationType() {
        return applicationType;
    }

    public void setApplicationType(ApplicationType applicationType) {
        this.applicationType = applicationType;
    }

    public CaseDetail getCaseDetail() {
        return caseDetail;
    }

    public void setCaseDetail(CaseDetail caseDetail) {
        this.caseDetail = caseDetail;
    }

    public LocalDate getDateReceived() {
        return dateReceived;
    }

    public void setDateReceived(LocalDate dateReceived) {
        this.dateReceived = dateReceived;
    }

    public String getOutOfTimeReason() {
        return outOfTimeReason;
    }

    public void setOutOfTimeReason(String outOfTimeReasons) {
        this.outOfTimeReason = outOfTimeReasons;
    }

    public boolean isOutOfTime() {
        return outOfTime;
    }

    public void setOutOfTime(boolean outOfTime) {
        this.outOfTime = outOfTime;
    }

    public JsonObject getInitiatedApplication() {
        return initiatedApplication;
    }

    public void setInitiatedApplication(final JsonObject initiatedApplication) {
        this.initiatedApplication = initiatedApplication;
    }

}