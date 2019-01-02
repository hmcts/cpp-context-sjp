package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
import uk.gov.moj.cpp.sjp.domain.common.PleaInformation;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

public class CaseView {

    private final String id;
    private final String urn;
    private final DefendantView defendant;
    private final ZonedDateTime dateTimeCreated;
    private final Set<CaseDocumentView> caseDocuments;
    private final ProsecutingAuthority prosecutingAuthority;
    private final Boolean completed;
    private final Boolean assigned;
    private final String reopenedInLibraReason;
    private final BigDecimal costs;
    private final LocalDate postingDate;
    private final LocalDate reopenedDate;
    private final String libraCaseNumber;
    private final String enterpriseId;
    private final boolean onlinePleaReceived;
    private final String datesToAvoid;
    private final CaseStatus status;
    private final Boolean listedInCriminalCourts;
    private final String hearingCourtName;
    private final ZonedDateTime hearingTime;

    public CaseView(final CaseDetail caseDetail) {

        this.id = caseDetail.getId().toString();
        this.urn = caseDetail.getUrn();
        this.prosecutingAuthority = caseDetail.getProsecutingAuthority();

        this.defendant = new DefendantView(caseDetail.getDefendant());
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
        //TODO SINGLE OFFENCE only implementation
        this.status = CaseStatus.calculateStatus(caseDetail.getPostingDate(),
                caseDetail.isAnyOffencePendingWithdrawal(),
                new PleaInformation(caseDetail.getFirstOffencePlea(), caseDetail.getFirstOffencePleaDate()),
                caseDetail.getDatesToAvoid(),
                caseDetail.isCompleted(),
                caseDetail.isReferredForCourtHearing(),
                caseDetail.getReopenedDate());
        this.listedInCriminalCourts = caseDetail.getListedInCriminalCourts();
        this.hearingCourtName = caseDetail.getHearingCourtName();
        this.hearingTime = caseDetail.getHearingTime();
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

    public ProsecutingAuthority getProsecutingAuthority() {
        return prosecutingAuthority;
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

    public boolean isOnlinePleaReceived() {
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

}
