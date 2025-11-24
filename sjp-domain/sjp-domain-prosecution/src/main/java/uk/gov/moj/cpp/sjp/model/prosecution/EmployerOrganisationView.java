package uk.gov.moj.cpp.sjp.model.prosecution;

public class EmployerOrganisationView {

    private final String name;
    private final AddressView address;
    private final ContactView contact;

    public EmployerOrganisationView(final String name,
                                    final AddressView address,
                                    final ContactView contact) {

        this.name = name;
        this.address = address;
        this.contact = contact;
    }

    public String getName() {
        return name;
    }

    public AddressView getAddress() {
        return address;
    }

    public ContactView getContact() {
        return contact;
    }
}
