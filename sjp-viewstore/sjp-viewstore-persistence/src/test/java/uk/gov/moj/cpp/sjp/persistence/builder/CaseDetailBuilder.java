package uk.gov.moj.cpp.sjp.persistence.builder;


import static java.time.ZoneOffset.UTC;

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

    public final UUID CASE_ID = UUID.randomUUID();
    private final String URN = CASE_ID.toString().toUpperCase();

    private UUID id = CASE_ID;
    private String urn = URN;
    private String ptiUrn;
    private String prosecutingAuthority = "CPS";
    private Set<CaseDocument> caseDocuments = new LinkedHashSet<>();
    private Set<DefendantDetail> defendants = new LinkedHashSet<>();
    private String initiationCode;
    private Boolean completed;
    private String summonsCode;
    private String libraOriginatingOrg;
    private BigDecimal costs;
    private LocalDate postingDate;
    private ZonedDateTime createdOn = ZonedDateTime.now(UTC);

    private CaseDetailBuilder() {
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

    public CaseDetailBuilder withPtiUrn(String ptiUrn) {
        this.ptiUrn = ptiUrn;
        return this;
    }

    public CaseDetailBuilder withProsecutingAuthority(String prosecutingAuthority) {
        this.prosecutingAuthority = prosecutingAuthority;
        return this;
    }

    public CaseDetailBuilder withCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }

    public CaseDetailBuilder withInitiationCode(String initiationCode) {
        this.initiationCode = initiationCode;
        return this;
    }

    public CaseDetailBuilder withLibraOriginatingOrg(String libraOriginatingOrg) {
        this.libraOriginatingOrg = libraOriginatingOrg;
        return this;
    }

    public CaseDetailBuilder withSummonsCode(String summonsCode) {
        this.summonsCode = summonsCode;
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

    public CaseDetailBuilder addDefendantDetail(DefendantDetail defendantDetail) {
        if (defendantDetail == null) {
            return this;
        }
        this.defendants.add(defendantDetail);
        return this;
    }

    public CaseDetailBuilder addCaseDocument(CaseDocument caseDocument) {
        if (caseDocument == null) {
            return this;
        }
        this.caseDocuments.add(caseDocument);
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
                createdOn);

        caseDetail.setLibraOriginatingOrg(libraOriginatingOrg);
        caseDetail.setPtiUrn(ptiUrn);
        caseDetail.setSummonsCode(summonsCode);
        caseDetail.setCosts(costs);
        caseDetail.setPostingDate(postingDate);

        defendants.forEach(d -> {
            d.setCaseDetail(caseDetail);
            caseDetail.addDefendant(d);
        });
        caseDocuments.forEach(d -> {
            caseDetail.addCaseDocuments(d);
        });

        return caseDetail;
    }
}
