package uk.gov.moj.cpp.sjp.event.processor.model.referral;


import java.time.LocalDate;

public class PersonDetailsView {

    private final String title;
    private final String firstName;
    private final String lastName;
    private final LocalDate dateOfBirth;
    private final String gender;
    private final String interpreterLanguageNeeds;
    private final AddressView address;
    private final ContactView contact;

    @SuppressWarnings("squid:S00107")
    public PersonDetailsView(final String title,
                             final String firstName,
                             final String lastName,
                             final LocalDate dateOfBirth,
                             final String gender,
                             final String interpreterLanguageNeeds,
                             final AddressView address,
                             final ContactView contact) {

        this.title = title;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.interpreterLanguageNeeds = interpreterLanguageNeeds;
        this.address = address;
        this.contact = contact;
    }

    public String getTitle() {
        return title;
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

    public String getGender() {
        return gender;
    }

    public String getInterpreterLanguageNeeds() {
        return interpreterLanguageNeeds;
    }

    public AddressView getAddress() {
        return address;
    }

    public ContactView getContact() {
        return contact;
    }
}
