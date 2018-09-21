package uk.gov.moj.cpp.sjp.persistence.entity;

public class AwaitingCase {

    private final String defendantFirstName;
    private final String defendantLastName;
    private final String offenceCode;

    public AwaitingCase(String defendantFirstName, String defendantLastName, String offenceCode) {
        this.defendantFirstName = defendantFirstName;
        this.defendantLastName = defendantLastName;
        this.offenceCode = offenceCode;
    }

    public String getDefendantFirstName() {
        return defendantFirstName;
    }

    public String getDefendantLastName() {
        return defendantLastName;
    }

    public String getOffenceCode() {
        return offenceCode;
    }
}
