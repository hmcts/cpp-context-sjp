package uk.gov.moj.cpp.sjp.query.view.response;

import static java.util.Objects.nonNull;

import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

public class CasePendingDatesToAvoidView {

    private final UUID caseId;

    private final ZonedDateTime pleaEntry;

    private final String firstName;

    private final String lastName;

    private final String legalEntityName;

    private final AddressView address;

    private final String referenceNumber;

    private final LocalDate dateOfBirth;

    private final String region;

    public CasePendingDatesToAvoidView(final PendingDatesToAvoid pendingDatesToAvoid) {
        this.caseId = pendingDatesToAvoid.getCaseId();
        this.pleaEntry = pendingDatesToAvoid.getPleaDate();
        this.firstName = nonNull(pendingDatesToAvoid.getCaseDetail().getDefendant().getPersonalDetails()) ? pendingDatesToAvoid.getCaseDetail().getDefendant().getPersonalDetails().getFirstName() : null;
        this.lastName = nonNull(pendingDatesToAvoid.getCaseDetail().getDefendant().getPersonalDetails()) ? pendingDatesToAvoid.getCaseDetail().getDefendant().getPersonalDetails().getLastName() : null;
        this.address = new AddressView(pendingDatesToAvoid.getCaseDetail().getDefendant().getAddress());
        this.referenceNumber = pendingDatesToAvoid.getCaseDetail().getUrn();
        this.dateOfBirth = nonNull(pendingDatesToAvoid.getCaseDetail().getDefendant().getPersonalDetails()) ? pendingDatesToAvoid.getCaseDetail().getDefendant().getPersonalDetails().getDateOfBirth() : null;
        this.region = pendingDatesToAvoid.getCaseDetail().getDefendant().getRegion();
        this.legalEntityName = nonNull(pendingDatesToAvoid.getCaseDetail().getDefendant().getLegalEntityDetails()) ? pendingDatesToAvoid.getCaseDetail().getDefendant().getLegalEntityDetails().getLegalEntityName() : null;
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

    public AddressView getAddress() {
        return address;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getRegion() {
        return region;
    }

    public String getLegalEntityName() {
        return legalEntityName;
    }
}
