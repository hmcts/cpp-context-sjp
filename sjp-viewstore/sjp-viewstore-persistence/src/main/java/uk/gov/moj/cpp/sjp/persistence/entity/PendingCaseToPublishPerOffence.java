package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.LocalDate;
import java.util.UUID;

public class PendingCaseToPublishPerOffence {

    private final String firstName;
    private final String lastName;
    private final String legalEntityName;
    private final LocalDate defendantDateOfBirth;
    private final UUID caseId;
    private String caseUrn;
    private final String addressLine1;
    private final String addressLine2;
    private final String addressLine3;
    private final String addressLine4;
    private final String addressLine5;
    private final String postcode;
    private final String offenceCode;
    private final LocalDate offenceStartDate;
    private final String offenceWording;
    private final Boolean pressRestrictionRequested;
    private final String pressRestrictionName;
    private final Boolean completed;
    private final String prosecutor;

    @SuppressWarnings("squid:S00107")
    public PendingCaseToPublishPerOffence(final String firstName, final String lastName, String legalEntityName, final LocalDate defendantDateOfBirth,
                                          final UUID caseId, final String caseUrn,
                                          final String addressLine1, final String addressLine2,
                                          final String addressLine3, final String addressLine4, final String addressLine5,
                                          final String postcode, final String offenceCode, final LocalDate offenceStartDate,
                                          final String offenceWording, final Boolean pressRestrictionRequested,
                                          final String pressRestrictionName, final Boolean completed, final String prosecutor) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.legalEntityName = legalEntityName;
        this.defendantDateOfBirth = defendantDateOfBirth;
        this.caseId = caseId;
        this.caseUrn = caseUrn;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressLine3 = addressLine3;
        this.addressLine4 = addressLine4;
        this.addressLine5 = addressLine5;
        this.postcode = postcode;
        this.offenceCode = offenceCode;
        this.offenceStartDate = offenceStartDate;
        this.offenceWording = offenceWording;
        this.prosecutor = prosecutor;
        this.pressRestrictionRequested = pressRestrictionRequested;
        this.pressRestrictionName = pressRestrictionName;
        this.completed = completed;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDefendantDateOfBirth() {
        return defendantDateOfBirth;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public String getAddressLine1() {
        return addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public String getAddressLine3() {
        return addressLine3;
    }

    public String getAddressLine4() {
        return addressLine4;
    }

    public String getAddressLine5() {
        return addressLine5;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getOffenceCode() {
        return offenceCode;
    }

    public LocalDate getOffenceStartDate() {
        return offenceStartDate;
    }

    public String getProsecutor() {
        return prosecutor;
    }

    public String getOffenceWording() {
        return offenceWording;
    }

    public Boolean getPressRestrictionRequested() {
        return pressRestrictionRequested;
    }

    public String getPressRestrictionName() {
        return pressRestrictionName;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }
}
