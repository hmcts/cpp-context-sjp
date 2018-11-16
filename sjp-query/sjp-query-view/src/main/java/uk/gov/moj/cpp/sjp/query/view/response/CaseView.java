package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.domain.common.CaseStatus;
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
    private BigDecimal costs;
    private LocalDate postingDate;
    private LocalDate reopenedDate;
    private String libraCaseNumber;
    private final String reopenedInLibraReason;
    private String enterpriseId;
    private boolean onlinePleaReceived;
    private String datesToAvoid;
    private CaseStatus status;
    private Boolean listedInCriminalCourts;

    public CaseView(CaseDetail caseDetail) {

        this.id = caseDetail.getId().toString();
        this.urn = caseDetail.getUrn();
        this.prosecutingAuthority = caseDetail.getProsecutingAuthority();

        this.defendant = new DefendantView(caseDetail.getDefendant());
        this.dateTimeCreated = caseDetail.getDateTimeCreated();

        this.caseDocuments = new LinkedHashSet<>();
        if (!caseDetail.getCaseDocuments().isEmpty()) {
            caseDetail.getCaseDocuments().forEach(caseDocument -> caseDocuments.add(new CaseDocumentView(caseDocument)));
        }

        completed = caseDetail.getCompleted();
        assigned = caseDetail.getAssigneeId() != null;

        this.costs = caseDetail.getCosts();
        this.postingDate = caseDetail.getPostingDate();
        this.reopenedDate = caseDetail.getReopenedDate();
        this.libraCaseNumber = caseDetail.getLibraCaseNumber();
        this.reopenedInLibraReason = caseDetail.getReopenedInLibraReason();
        this.enterpriseId = caseDetail.getEnterpriseId();
        this.onlinePleaReceived = Boolean.TRUE.equals(caseDetail.getOnlinePleaReceived());
        this.datesToAvoid = caseDetail.getDatesToAvoid();
        this.status = caseDetail.getStatus();
        this.listedInCriminalCourts = caseDetail.getListedInCriminalCourts();
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
}
