package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import java.util.UUID;

public class EmployerOrganisationView {

    private final UUID id;
    private final String name;
    private final AddressView address;
    private final ContactView contact;

    public EmployerOrganisationView(final UUID id,
                                    final String name,
                                    final AddressView address,
                                    final ContactView contact) {

        this.id = id;
        this.name = name;
        this.address = address;
        this.contact = contact;
    }

    public UUID getId() {
        return id;
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
