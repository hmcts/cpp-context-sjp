package uk.gov.moj.cpp.sjp.domain.transformation.notes;

public class UserDetails {

    private String firstName;
    private String lastName;

    public UserDetails(final String firstName, final String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

}
