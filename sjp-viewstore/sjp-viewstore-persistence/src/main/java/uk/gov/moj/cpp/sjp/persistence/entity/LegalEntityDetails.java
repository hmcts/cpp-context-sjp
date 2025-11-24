package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.Defendant;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class LegalEntityDetails implements Serializable {

    private static final long serialVersionUID = 2405172041950251807L;

    @Column(name = "legal_entity_name")
    private String legalEntityName;

    public LegalEntityDetails() {
    }

    public LegalEntityDetails(final String legalEntityName) {
        this.legalEntityName = legalEntityName;

    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public void setLegalEntityName(final String legalEntityName) {
        this.legalEntityName = legalEntityName;
    }

    public LegalEntityDetails builder(final Defendant defendant) {
        LegalEntityDetails legalEntityDetails = new LegalEntityDetails();
        legalEntityDetails.setLegalEntityName(defendant.getLegalEntityName());
        return legalEntityDetails;
    }
}
