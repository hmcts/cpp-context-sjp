package uk.gov.moj.sjp.it.command.builder;

public class ContactDetailsBuilder {

    private final String email;
    private final String email2;
    private final String home;
    private final String mobile;
    private final String business;

    private ContactDetailsBuilder() {
        email = "email@email.com";
        email2 = "email2@email.com";
        home = "02087654321";
        mobile = "07123456789";
        business = "07123456999";
    }

    public static ContactDetailsBuilder withDefaults() {
        return new ContactDetailsBuilder();
    }

    public String getEmail() {
        return email;
    }

    public String getEmail2() {
        return email2;
    }

    public String getHome() {
        return home;
    }

    public String getMobile() {
        return mobile;
    }

    public String getBusiness() {
        return business;
    }

}
