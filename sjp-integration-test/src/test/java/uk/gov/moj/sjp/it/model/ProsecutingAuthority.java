package uk.gov.moj.sjp.it.model;


public enum ProsecutingAuthority {

    CPS("Crown Prosecution Service"),
    TVL("TV License"),
    DVLA("Driver and Vehicle Licensing Agency"),
    TFL("Transport for London"),
    POLICE("Police"),
    METLI("MetroLink");

    private String fullName;

    ProsecutingAuthority(final String fullName) {
        this.fullName = fullName;
    }

    public String getFullName() {
        return fullName;
    }

}
