package uk.gov.moj.cpp.sjp.query.view.response;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;

public class LegalEntityDetailsView {

    private String legalEntityName;

    private AddressView address;

    private ContactDetailsView contactDetails;

    private Boolean addressChanged;

    private Boolean nameChanged;


    public LegalEntityDetailsView(final DefendantDetail defendantDetail) {
        final LegalEntityDetails legalEntityDetails=defendantDetail.getLegalEntityDetails();
        if (legalEntityDetails != null) {
            this.legalEntityName = legalEntityDetails.getLegalEntityName();
            this.address = ofNullable(defendantDetail.getAddress()).map(AddressView::new).orElse(null);
            this.contactDetails = new ContactDetailsView(defendantDetail.getContactDetails());
            this.addressChanged = nonNull(defendantDetail.getAddressUpdatedAt());
            this.nameChanged = nonNull(defendantDetail.getNameUpdatedAt());
        }
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }

    public AddressView getAddress() {
        return address;
    }

    public ContactDetailsView getContactDetails() {
        return contactDetails;
    }

    public Boolean isNameChanged() {
        return nameChanged;
    }

    public Boolean isAddressChanged() {
        return addressChanged;
    }
}