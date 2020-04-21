package uk.gov.moj.cpp.sjp.persistence.entity;

import uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus;

import java.time.ZonedDateTime;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class CaseNotGuiltyPlea {

    private final UUID id;

    private final String urn;

    private final ZonedDateTime pleaDate;

    private final String firstName;

    private final String lastName;

    private final String prosecutingAuthority;

    private final CaseManagementStatus caseManagementStatus;

    public CaseNotGuiltyPlea(final UUID id,
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CaseNotGuiltyPlea that = (CaseNotGuiltyPlea) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .append(urn, that.urn)
                .append(pleaDate, that.pleaDate)
                .append(firstName, that.firstName)
                .append(lastName, that.lastName)
                .append(prosecutingAuthority, that.prosecutingAuthority)
                .append(caseManagementStatus, that.caseManagementStatus)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(urn)
                .append(pleaDate)
                .append(firstName)
                .append(lastName)
                .append(prosecutingAuthority)
                .append(caseManagementStatus)
                .toHashCode();
    }
}
