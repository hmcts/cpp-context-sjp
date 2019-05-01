package uk.gov.moj.cpp.sjp.event.processor.model.referral;

import static org.apache.commons.lang3.StringUtils.isBlank;

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

        this.home = nullifyIfBlank(home);
        this.work = nullifyIfBlank(work);
        this.mobile = nullifyIfBlank(mobile);
        this.primaryEmail = nullifyIfBlank(primaryEmail);
        this.secondaryEmail = nullifyIfBlank(secondaryEmail);
    }

    private static String nullifyIfBlank(final String value) {
        return isBlank(value) ? null : value;
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
