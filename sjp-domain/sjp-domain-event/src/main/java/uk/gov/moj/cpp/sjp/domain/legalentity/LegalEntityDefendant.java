package uk.gov.moj.cpp.sjp.domain.legalentity;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;

@SuppressWarnings("PMD.BeanMembersShouldSerialize")
public class LegalEntityDefendant {
    private final String name;
    private final Address address;
    private final ContactDetails contactDetails;
    private final String incorporationNumber;
    private final String position;

    public LegalEntityDefendant(String name, Address address, ContactDetails contactDetails, String incorporationNumber, String position) {
        this.name =  name;
        this.address = address;
        this.contactDetails =  contactDetails;
        this.incorporationNumber = incorporationNumber;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public Address getAddress() {
        return address;
    }

    public ContactDetails getContactDetails() {
        return contactDetails;
    }

    public String getIncorporationNumber() {
        return incorporationNumber;
    }

    public String getPosition() {
        return position;
    }

    public static LegalEntityDefendant.Builder legalEntityDefendant() {
        return new LegalEntityDefendant.Builder();
    }

    public static class Builder {
        private String name;
        private Address address;
        private ContactDetails contactDetails;
        private String incorporationNumber;
        private String position;

        public LegalEntityDefendant.Builder withName(final String name) {
            this.name = name;
            return this;
        }

        public LegalEntityDefendant.Builder withAdddres(final Address address) {
            this.address = address;
            return this;
        }

        public LegalEntityDefendant.Builder withContactDetails(final ContactDetails contactDetails) {
            this.contactDetails = contactDetails;
            return this;
        }

        public LegalEntityDefendant.Builder withIncorporationNumber(final String incorporationNumber) {
            this.incorporationNumber = incorporationNumber;
            return this;
        }

        public LegalEntityDefendant.Builder withPosition(final String position) {
            this.position = position;
            return this;
        }

        public LegalEntityDefendant.Builder withValuesFrom(final LegalEntityDefendant legalEntityDefendant) {
            this.address = legalEntityDefendant.getAddress();
            this.incorporationNumber = legalEntityDefendant.getIncorporationNumber();
            this.name = legalEntityDefendant.getName();
            this.contactDetails = legalEntityDefendant.getContactDetails();
            this.position = legalEntityDefendant.getPosition();
            return this;
        }

        public LegalEntityDefendant build() {
            return new LegalEntityDefendant(name, address, contactDetails, incorporationNumber, position);
        }
    }
}
