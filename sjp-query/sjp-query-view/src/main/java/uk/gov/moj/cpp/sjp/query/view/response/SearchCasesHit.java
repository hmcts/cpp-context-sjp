package uk.gov.moj.cpp.sjp.query.view.response;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.time.LocalDate;

public class SearchCasesHit {

    private final String id;
    private final String urn;
    private final LocalDate postingDate;
    private final LocalDate reopenedDate;
    private final Boolean completed;
    private final String plea;


    public SearchCasesHit(final String id, final String urn, final LocalDate postingDate, LocalDate reopenedDate, final Boolean completed, final String plea) {
        this.id = id;
        this.urn = urn;
        this.postingDate = postingDate;
        this.reopenedDate = reopenedDate;
        this.completed = completed;
        this.plea = plea;
    }

    public SearchCasesHit(final CaseDetail caseDetail) {
        id = caseDetail.getId().toString();
        urn = caseDetail.getUrn();
        postingDate = caseDetail.getPostingDate();
        reopenedDate = caseDetail.getReopenedDate();
        completed = caseDetail.getCompleted();
        this.plea = caseDetail.getDefendants().stream().map(d -> d.getOffences()).flatMap(s -> s.stream())
                .filter(o -> isNotEmpty(o.getPlea())).findFirst().map(OffenceDetail::getPlea).orElse("");
    }

    public String getId() {
        return id;
    }

    public String getUrn() {
        return urn;
    }

    public LocalDate getPostingDate() {
        return postingDate;
    }

    public LocalDate getReopenedDate() {
        return reopenedDate;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public String getPlea() {
        return plea;
    }

    @Override
    public String toString() {
        return "SearchCasesHit{" +
                "id='" + id + '\'' +
                ", urn='" + urn + '\'' +
                ", postingDate=" + postingDate +
                ", reopenedDate=" + reopenedDate +
                ", completed=" + completed +
                ", plea='" + plea + '\'' +
                '}';
    }
}
