package uk.gov.moj.cpp.sjp.persistence.builder;

import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;

public class LegalEntityDetailsBuilder {

    private String legalEntityName;

    public LegalEntityDetailsBuilder() {
    }

    public static LegalEntityDetailsBuilder buildLegalEntityDetails() {
        return new LegalEntityDetailsBuilder();
    }

    public LegalEntityDetailsBuilder withLegalEntityName(String legalEntityName) {
        this.legalEntityName = legalEntityName;
        return this;
    }

    public LegalEntityDetails build() {
        return new LegalEntityDetails(legalEntityName);

    }
}
