package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class CasePendingDatesToAvoidView {

    private final UUID caseId;

    private final ZonedDateTime pleaEntry;

    private final String firstName;

    private final String lastName;

    private final PersonalAddressView address;

    private final String referenceNumber;

    private final LocalDate dateOfBirth;

    public CasePendingDatesToAvoidView(final PendingDatesToAvoid pendingDatesToAvoid) {
        this.caseId = pendingDatesToAvoid.getCaseId();
        this.pleaEntry = pendingDatesToAvoid.getPleaDate();
        this.firstName = pendingDatesToAvoid.getCaseDetail().getDefendant().getPersonalDetails().getFirstName();
        this.lastName = pendingDatesToAvoid.getCaseDetail().getDefendant().getPersonalDetails().getLastName();
        this.address = new PersonalAddressView(pendingDatesToAvoid.getCaseDetail().getDefendant().getPersonalDetails().getAddress());
        this.referenceNumber = pendingDatesToAvoid.getCaseDetail().getUrn();
        this.dateOfBirth = pendingDatesToAvoid.getCaseDetail().getDefendant().getPersonalDetails().getDateOfBirth();
    }

    public UUID getCaseId() {
        return caseId;
    }

    public ZonedDateTime getPleaEntry() {
        return pleaEntry;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public PersonalAddressView getAddress() {
        return address;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }
}
