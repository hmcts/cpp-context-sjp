package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.LocalDate;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class CaseWithoutDefendantPostcode {

    private final UUID id;

    private final String urn;

    private final LocalDate postingDate;

    private final String firstName;

    private final String lastName;

    private final String prosecutingAuthority;


    public CaseWithoutDefendantPostcode(final UUID id,
                                        final String urn,
                                        final LocalDate postingDate,
                                        final String firstName,
                                        final String lastName,
                                        final String prosecutingAuthority) {
        this.id = id;
        this.urn = urn;
        this.postingDate = postingDate;
        this.firstName = firstName;
        this.lastName = lastName;
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getPostingDate() {
        return postingDate;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final CaseWithoutDefendantPostcode that = (CaseWithoutDefendantPostcode) o;

        return new EqualsBuilder()
                .append(id, that.id)
                .append(urn, that.urn)
                .append(postingDate, that.postingDate)
                .append(firstName, that.firstName)
                .append(lastName, that.lastName)
                .append(prosecutingAuthority, that.prosecutingAuthority)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(urn)
                .append(postingDate)
                .append(firstName)
                .append(lastName)
                .append(prosecutingAuthority)
                .toHashCode();
    }
}
