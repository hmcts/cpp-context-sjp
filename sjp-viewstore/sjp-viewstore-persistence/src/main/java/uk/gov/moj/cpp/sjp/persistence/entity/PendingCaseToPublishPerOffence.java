package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.LocalDate;
import java.util.UUID;

public class PendingCaseToPublishPerOffence {

    private final String firstName;
    private final String lastName;
    private final UUID caseId;
    private final String addressLine3;
    private final String addressLine4;
    private final String addressLine5;
    private final String postcode;
    private final String offenceCode;
    private final LocalDate offenceStartDate;
    private final String prosecutor;

    @SuppressWarnings("squid:S00107")
    public PendingCaseToPublishPerOffence(final String firstName, final String lastName,
                                          final UUID caseId,
                                          final String addressLine3, final String addressLine4, final String addressLine5,
                                          final String postcode, final String offenceCode, final LocalDate offenceStartDate,
                                          final String prosecutor) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.caseId = caseId;
        this.addressLine3 = addressLine3;
        this.addressLine4 = addressLine4;
        this.addressLine5 = addressLine5;
        this.postcode = postcode;
        this.offenceCode = offenceCode;
        this.offenceStartDate = offenceStartDate;
        this.prosecutor = prosecutor;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public UUID getCaseId() {
        return caseId;
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
}
