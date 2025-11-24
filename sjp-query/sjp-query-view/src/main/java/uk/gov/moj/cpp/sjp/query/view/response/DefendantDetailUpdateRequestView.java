package uk.gov.moj.cpp.sjp.query.view.response;


import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.sjp.persistence.entity.Address;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetailUpdateRequest;

import java.time.LocalDate;
import java.util.UUID;

public class DefendantDetailUpdateRequestView {

    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final String legalEntityName;
    private final UUID defendantId;
    private final AddressView address;
    private final DefendantDetailUpdateRequest.Status status;
    private final boolean nameUpdated;
    private final boolean addressUpdated;
    private final boolean dobUpdated;

    @SuppressWarnings("PMD.NullAssignment")
    public DefendantDetailUpdateRequestView(DefendantDetailUpdateRequest defendantDetailUpdateRequest) {

        this.firstName = defendantDetailUpdateRequest.isNameUpdated() ? defendantDetailUpdateRequest.getFirstName() : null;
        this.lastName = defendantDetailUpdateRequest.isNameUpdated() ? defendantDetailUpdateRequest.getLastName() : null;
        this.dateOfBirth = defendantDetailUpdateRequest.isDobUpdated() ? defendantDetailUpdateRequest.getDateOfBirth() : null;
        this.legalEntityName = defendantDetailUpdateRequest.isNameUpdated() ? defendantDetailUpdateRequest.getLegalEntityName() : null;
        this.status = nonNull(defendantDetailUpdateRequest.getStatus()) ? defendantDetailUpdateRequest.getStatus() : null;
        this.nameUpdated = defendantDetailUpdateRequest.isNameUpdated();
        this.addressUpdated = defendantDetailUpdateRequest.isAddressUpdated();
        this.dobUpdated = defendantDetailUpdateRequest.isDobUpdated();
        this.defendantId = nonNull(defendantDetailUpdateRequest.getDefendantId()) ? defendantDetailUpdateRequest.getDefendantId() : null;
        this.address = defendantDetailUpdateRequest.isAddressUpdated() ? new AddressView(new Address(defendantDetailUpdateRequest.getAddress1(), defendantDetailUpdateRequest.getAddress2(), defendantDetailUpdateRequest.getAddress3(), defendantDetailUpdateRequest.getAddress4(), defendantDetailUpdateRequest.getAddress5(), defendantDetailUpdateRequest.getPostcode())): null;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public DefendantDetailUpdateRequest.Status getStatus() {
        return status;
    }

    public boolean isNameUpdated() {
        return nameUpdated;
    }

    public boolean isAddressUpdated() {
        return addressUpdated;
    }

    public boolean isDobUpdated() {
        return dobUpdated;
    }

    public AddressView getAddress() {
        return address;
    }

    public UUID getDefendantId() {
        return defendantId;
    }
}
