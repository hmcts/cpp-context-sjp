package uk.gov.moj.cpp.sjp.event.processor.model.referral;

public class DefendantAliasView {

    private String title;
    private String firstName;
    private String middleName;
    private String lastName;

    public DefendantAliasView(final String title,
                              final String firstName,
                              final String middleName,
                              final String lastName) {
        this.title = title;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
    }

    public String getTitle() {
        return title;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }
}
