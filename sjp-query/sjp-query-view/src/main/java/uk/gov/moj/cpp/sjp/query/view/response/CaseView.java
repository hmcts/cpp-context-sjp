package uk.gov.moj.cpp.sjp.query.view.response;


import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.nonNull;

import com.google.common.collect.ImmutableList;

import javax.json.JsonObject;

public class CaseView {

    private final String id;
    private final String urn;
    private final DefendantView defendant;
    private final List<CaseDecisionView> caseDecisions;
    private final ZonedDateTime dateTimeCreated;
    private final Set<CaseDocumentView> caseDocuments;
    private final String prosecutingAuthority;
    private final String prosecutingAuthorityName;
    private final Boolean completed;
    private final Boolean assigned;
    private final String reopenedInLibraReason;
    private final BigDecimal costs;
    private final LocalDate postingDate;
    private final LocalDate reopenedDate;
    private final String libraCaseNumber;
    private final String enterpriseId;
    private final Boolean onlinePleaReceived;
    private final String datesToAvoid;
    private final CaseStatus status;
    private final Boolean listedInCriminalCourts;
    private final String hearingCourtName;
    private final ZonedDateTime hearingTime;
    private final LocalDate adjournedTo;
    private final Boolean policeFlag;
    private final Boolean postConviction;
    private final Boolean setAside;

    public CaseView(final CaseDetail caseDetail, final JsonObject prosecutor) {

        this.postConviction = caseDetail.getDefendant().getOffences().stream().anyMatch(
                offence -> nonNull(offence.getConviction()));

        this.id = caseDetail.getId().toString();
        this.urn = caseDetail.getUrn();

        this.prosecutingAuthority = caseDetail.getProsecutingAuthority();
        this.prosecutingAuthorityName = prosecutor.getString("fullName");
        this.policeFlag = prosecutor.getBoolean("policeFlag", false);

        this.defendant = new DefendantView(caseDetail.getDefendant());
        this.caseDecisions = new ArrayList<>();
        caseDetail.getCaseDecisions().forEach(caseDecisionEntity -> caseDecisions.add(new CaseDecisionView(caseDecisionEntity)));
        this.dateTimeCreated = caseDetail.getDateTimeCreated();

        this.caseDocuments = new LinkedHashSet<>();
        if (!caseDetail.getCaseDocuments().isEmpty()) {
            caseDetail.getCaseDocuments().forEach(caseDocument -> caseDocuments.add(new CaseDocumentView(caseDocument)));
        }

        completed = caseDetail.isCompleted();
        assigned = caseDetail.getAssigneeId() != null;

        this.costs = caseDetail.getCosts();
        this.postingDate = caseDetail.getPostingDate();
        this.reopenedDate = caseDetail.getReopenedDate();
        this.libraCaseNumber = caseDetail.getLibraCaseNumber();
        this.reopenedInLibraReason = caseDetail.getReopenedInLibraReason();
        this.enterpriseId = caseDetail.getEnterpriseId();
        this.onlinePleaReceived = Boolean.TRUE.equals(caseDetail.getOnlinePleaReceived());
        this.datesToAvoid = caseDetail.getDatesToAvoid();
        this.adjournedTo = caseDetail.getAdjournedTo();

        this.status = caseDetail.getCaseStatus();
        this.listedInCriminalCourts = caseDetail.getListedInCriminalCourts();
        this.hearingCourtName = caseDetail.getHearingCourtName();
        this.hearingTime = caseDetail.getHearingTime();
        this.setAside = caseDetail.getSetAside();
    }

    public String getId() {
        return id;
    }

    public String getUrn() {
        return urn;
    }

    public Set<CaseDocumentView> getCaseDocuments() {
        return caseDocuments;
    }

    public DefendantView getDefendant() {
        return defendant;
    }

    public List<CaseDecisionView> getCaseDecisions() {
        return ImmutableList.copyOf(caseDecisions);
    }

    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public String getProsecutingAuthorityName() {
        return prosecutingAuthorityName;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public Boolean getAssigned() {
        return assigned;
    }

    public ZonedDateTime getDateTimeCreated() {
        return dateTimeCreated;
    }

    public BigDecimal getCosts() {
        return costs;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public LocalDate getReopenedDate() {
        return reopenedDate;
    }

    public String getReopenedInLibraReason() {
        return reopenedInLibraReason;
    }

    public String getLibraCaseNumber() {
        return libraCaseNumber;
    }

    public String getEnterpriseId() {
        return enterpriseId;
    }

    public Boolean isOnlinePleaReceived() {
        return onlinePleaReceived;
    }

    public String getDatesToAvoid() {
        return datesToAvoid;
    }

    public CaseStatus getStatus() {
        return status;
    }

    public Boolean getListedInCriminalCourts() {
        return listedInCriminalCourts;
    }

    public String getHearingCourtName() {
        return hearingCourtName;
    }

    public ZonedDateTime getHearingTime() {
        return hearingTime;
    }

    public LocalDate getAdjournedTo() {
        return adjournedTo;
    }

    public Boolean getPoliceFlag() {
        return policeFlag;
    }

    public Boolean getPostConviction() {
        return postConviction;
    }

    public Boolean isSetAside() {
        return setAside;
    }

}
