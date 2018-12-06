package uk.gov.moj.cpp.sjp.event.processor.model.referral;

public class ContactView {

    private final String home;
    private final String work;
    private final String mobile;
    private final String primaryEmail;
    private final String secondaryEmail;

    public ContactView(final String home,
                       final String work,
                       final String mobile,
                       final String primaryEmail,
                       final String secondaryEmail) {

        this.home = home;
        this.work = work;
        this.mobile = mobile;
        this.primaryEmail = primaryEmail;
        this.secondaryEmail = secondaryEmail;
    }

    public ContactView(final String work) {
        this(null, work, null, null, null);
    }

    public String getHome() {
        return home;
    }

    public String getWork() {
        return work;
    }

    public String getMobile() {
        return mobile;
    }

    public String getPrimaryEmail() {
        return primaryEmail;
    }

    public String getSecondaryEmail() {
        return secondaryEmail;
    }

}
