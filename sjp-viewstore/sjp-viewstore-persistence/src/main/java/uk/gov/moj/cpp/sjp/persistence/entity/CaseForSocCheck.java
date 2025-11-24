package uk.gov.moj.cpp.sjp.persistence.entity;

import java.time.ZonedDateTime;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class CaseForSocCheck {

    private final String id;

    private final String urn;

    private final ZonedDateTime lastUpdatedDate;

    private final String magistrate;

    private final String legalAdvisor;

    private final String prosecutingAuthority;


    public CaseForSocCheck(final String id,
                           final String urn,
                           final ZonedDateTime lastUpdatedDate,
                           final String magistrate,
                           final String legalAdvisor,
                           final String prosecutingAuthority) {
        this.id = id;
        this.urn = urn;
        this.lastUpdatedDate = lastUpdatedDate;
        this.magistrate = magistrate;
        this.legalAdvisor = legalAdvisor;
        this.prosecutingAuthority = prosecutingAuthority;
    }

    public String getId() {
        return id;
    }

    public String getUrn() {
        return urn;
    }

    public ZonedDateTime getLastUpdatedDate() {
        return lastUpdatedDate;
    }

    public String getMagistrate() {
        return magistrate;
    }

    public String getLegalAdvisor() {
        return legalAdvisor;
    }

    public String getProsecutingAuthority() {
        return prosecutingAuthority;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final CaseForSocCheck that = (CaseForSocCheck) o;
        return new EqualsBuilder()
                .append(id, that.id)
                .append(urn, that.urn)
                .append(lastUpdatedDate, that.lastUpdatedDate)
                .append(magistrate, that.magistrate)
                .append(legalAdvisor, that.legalAdvisor)
                .append(prosecutingAuthority, that.prosecutingAuthority)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(id)
                .append(urn)
                .append(lastUpdatedDate)
                .append(magistrate)
                .append(legalAdvisor)
                .append(prosecutingAuthority)
                .toHashCode();
    }

}
