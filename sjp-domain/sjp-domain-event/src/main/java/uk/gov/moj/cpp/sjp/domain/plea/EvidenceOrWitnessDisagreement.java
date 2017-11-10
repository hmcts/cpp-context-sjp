package uk.gov.moj.cpp.sjp.domain.plea;

public class EvidenceOrWitnessDisagreement {
    private String details;

    public EvidenceOrWitnessDisagreement() {
        //default constructor
    }

    public EvidenceOrWitnessDisagreement(String details) {
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}
