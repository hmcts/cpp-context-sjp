package uk.gov.moj.cpp.sjp.query.view.response;

import uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

public class CaseNotGuiltyPleaView {

    private final UUID id;

    private final String urn;

    private final ZonedDateTime pleaDate;

    private final String firstName;

    private final String lastName;

    private final String prosecutingAuthority;

    private final CaseManagementStatus caseManagementStatus;

    public CaseNotGuiltyPleaView(final UUID id,
                                 final String urn,
                                 final ZonedDateTime pleaDate,
                                 final String firstName,
                                 final String lastName,
                                 final String prosecutingAuthority,
                                 final CaseManagementStatus caseManagementStatus) {
        this.id = id;
        this.urn = urn;
        this.pleaDate = pleaDate;
        this.firstName = firstName;
        this.lastName = lastName;
        this.prosecutingAuthority = prosecutingAuthority;
        this.caseManagementStatus = caseManagementStatus;
    }

    public UUID getId() {
        return id;
    }

    public ZonedDateTime getPleaDate() {
        return pleaDate;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUrn() {
        return urn;
    }

    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    public CaseManagementStatus getCaseManagementStatus() {
        return caseManagementStatus;
    }
}
