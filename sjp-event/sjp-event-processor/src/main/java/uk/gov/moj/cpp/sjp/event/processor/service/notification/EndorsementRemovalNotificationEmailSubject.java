package uk.gov.moj.cpp.sjp.event.processor.service.notification;

import static java.util.Optional.ofNullable;

public class EndorsementRemovalNotificationEmailSubject {

    private final String courtName;
    private final String firstName;
    private final String lastName;
    private final String dateOfBirth;
    private final String caseUrn;

    public EndorsementRemovalNotificationEmailSubject(final String courtName,
                                                      final String firstName,
                                                      final String lastName,
                                                      final String dateOfBirth,
                                                      final String caseUrn) {
        this.courtName = courtName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = ofNullable(dateOfBirth).orElse("unknown");
        this.caseUrn = caseUrn;
    }

    @Override
    public String toString() {
        return String.format("Notification to DVLA to Remove Endorsement: %s; %s %s; %s; %s:",
                courtName, firstName, lastName, dateOfBirth, caseUrn);
    }
}
