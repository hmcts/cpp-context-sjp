package uk.gov.moj.cpp.sjp.domain;


public enum ProsecutingAuthority {

    CPS("Crown Prosecution Service"),
    TVL("TV License"),
    DVLA("Driver and Vehicle Licensing Agency"),
    TFL("Transport for London");

    private String fullName;

    ProsecutingAuthority(final String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

}
