package uk.gov.moj.cpp.sjp.persistence.builder;


import static java.time.ZoneOffset.UTC;

import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public class CaseDetailBuilder {

    private UUID id = UUID.randomUUID();
    private String urn = id.toString().toUpperCase();
    private ProsecutingAuthority prosecutingAuthority;
    private Set<CaseDocument> caseDocuments = new LinkedHashSet<>();
    private DefendantDetail defendant;
    //TODO no longer used
    private String initiationCode;
    private Boolean completed;
    private UUID assigneeId;
    private BigDecimal costs;
    private LocalDate postingDate;
    private String datesToAvoid;
    private ZonedDateTime createdOn = ZonedDateTime.now(UTC);

    private CaseDetailBuilder() {
        this.defendant = new DefendantDetail();
        this.prosecutingAuthority = ProsecutingAuthority.CPS;
    }

    public static CaseDetailBuilder aCase() {
        return new CaseDetailBuilder();
    }

    public CaseDetailBuilder withCaseId(UUID caseId) {
        this.id = caseId;
        return this;
    }

    public CaseDetailBuilder withUrn(String urn) {
        this.urn = urn;
        return this;
    }

    public CaseDetailBuilder withProsecutingAuthority(ProsecutingAuthority prosecutingAuthority) {
        this.prosecutingAuthority = prosecutingAuthority;
        return this;
    }

    public CaseDetailBuilder withCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }

    public CaseDetailBuilder withAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
        return this;
    }

    public CaseDetailBuilder withInitiationCode(String initiationCode) {
        this.initiationCode = initiationCode;
        return this;
    }

    public CaseDetailBuilder withCosts(BigDecimal costs) {
        this.costs = costs;
        return this;
    }

    public CaseDetailBuilder withPostingDate(LocalDate postingDate) {
        this.postingDate = postingDate;
        return this;
    }

    public CaseDetailBuilder withDatesToAvoid(String datesToAvoid) {
        this.datesToAvoid = datesToAvoid;
        return this;
    }

    public CaseDetailBuilder addDefendantDetail(DefendantDetail defendantDetail) {
        if (defendantDetail != null) {
            this.defendant = defendantDetail;
        }

        return this;
    }

    public CaseDetailBuilder addCaseDocument(CaseDocument caseDocument) {
        if (caseDocument != null) {
            this.caseDocuments.add(caseDocument);
        }

        return this;
    }

    public CaseDetailBuilder withCreatedOn(ZonedDateTime createdOn) {
        this.createdOn = createdOn;
        return this;
    }

    public CaseDetail build() {
        CaseDetail caseDetail = new CaseDetail(
                id,
                urn,
                prosecutingAuthority,
                initiationCode,
                completed,
                assigneeId,
                createdOn, defendant, costs, postingDate);
        caseDetail.setDatesToAvoid(datesToAvoid);

        caseDocuments.forEach(caseDetail::addCaseDocuments);

        return caseDetail;
    }
}
