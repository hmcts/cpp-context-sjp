package uk.gov.moj.cpp.sjp.domain.plea;

public class GuiltyPlea {

    private final boolean court;
    private final String mitigation;

    public GuiltyPlea(boolean court, String mitigation) {
        this.court = court;
        this.mitigation = mitigation;
    }

    public boolean getCourt() {
        return court;
    }

    public String getMitigation() {
        return mitigation;
    }
}