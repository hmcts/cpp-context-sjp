package uk.gov.moj.sjp.it.command.builder;

public class ContactDetailsBuilder {
    String email;
    String homeNumber;
    String mobile;

    private ContactDetailsBuilder() {
        email = "email@email.com";
        homeNumber = "02087654321";
        mobile = "07123456789";
    }
    public static ContactDetailsBuilder withDefaults() {
        return new ContactDetailsBuilder();
    }

    public String getEmail() {
        return email;
    }

    public String getHomeNumber() {
        return homeNumber;
    }

    public String getMobile() {
        return mobile;
    }
}
