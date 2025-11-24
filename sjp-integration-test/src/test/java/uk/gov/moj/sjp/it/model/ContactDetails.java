package uk.gov.moj.sjp.it.model;


import lombok.Builder;

@Builder
public class ContactDetails {

    @Builder.Default
    public String home = "0207 000 999";

    @Builder.Default
    public String mobile = "00000 000 000";

    @Builder.Default
    public String business = "11111 111 111";

    @Builder.Default
    public String email = "contact.details@here.com";

    @Builder.Default
    public String email2 = "contact.details2@here.com";

}
