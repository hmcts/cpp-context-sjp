package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.ApplicationStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.ApplicationType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplication;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseApplicationDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

public class ApplicationView {

    private final UUID applicationId;

    private final UUID parentApplicationId;

    private final String applicationReference;

    private final ApplicationStatus applicationStatus;

    private final String typeCode;

    private final UUID typeId;

    private final ApplicationType applicationType;

    private final LocalDate dateReceived;

    private final  String outOfTimeReason;

    private final boolean outOfTime;

    private final JsonObject initiatedApplication;

    private ApplicationDecisionView applicationDecision;

    public ApplicationView(CaseApplication caseApplication) {
        this.applicationId = caseApplication.getApplicationId();
        this.parentApplicationId = caseApplication.getParentApplicationId();
        this.applicationReference = caseApplication.getApplicationReference();
        this.applicationStatus = caseApplication.getApplicationStatus();
        this.typeCode = caseApplication.getTypeCode();
        this.typeId = caseApplication.getTypeId();
        this.applicationType = caseApplication.getApplicationType();
        this.dateReceived = caseApplication.getDateReceived();
        this.outOfTimeReason = caseApplication.getOutOfTimeReason();
        this.outOfTime = caseApplication.isOutOfTime();
        this.initiatedApplication  = caseApplication.getInitiatedApplication();
        final CaseApplicationDecision caseApplicationDecision = caseApplication.getApplicationDecision();
        if (caseApplicationDecision != null) {
            this.applicationDecision = new ApplicationDecisionView(caseApplicationDecision);
            setPreviousFinalDecision(caseApplication.getApplicationDecision(),
                    caseApplication.getCaseDetail().getCaseDecisions());
        }
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public UUID getParentApplicationId() {
        return parentApplicationId;
    }

    public String getApplicationReference() {
        return applicationReference;
    }

    public ApplicationStatus getApplicationStatus() { return applicationStatus; }

    public String getTypeCode() {
        return typeCode;
    }

    public UUID getTypeId() {
        return typeId;
    }

    public ApplicationType getApplicationType() {
        return applicationType;
    }

    public LocalDate getDateReceived() {
        return dateReceived;
    }

    public String getOutOfTimeReason() {
        return outOfTimeReason;
    }

    public boolean isOutOfTime() {
        return outOfTime;
    }

    public JsonObject getInitiatedApplication() {
        return initiatedApplication;
    }

    public ApplicationDecisionView getApplicationDecision() {
        return applicationDecision;
    }

    private void setPreviousFinalDecision(final CaseApplicationDecision caseApplicationDecision,
                                                      final List<CaseDecision>caseDecisions) {
        caseDecisions
                .stream()
                .filter(caseDecisionView -> caseDecisionView.getSavedAt().isBefore(caseApplicationDecision.getSavedAt()))
                .map(CaseDecisionView::new)
                .max(CaseDecisionView::compareTo)
                .ifPresent(e -> applicationDecision.setPreviousFinalDecision(e));
    }

}
