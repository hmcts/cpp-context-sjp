package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import java.io.Serializable;
import java.time.LocalDate;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class EnforcementPendingApplicationNotificationTemplateData implements Serializable {

    private final String gobAccountNumber;
    private final int divisionCode;
    private final String caseReference;
    private final LocalDate dateApplicationIsListed;
    private final String defendantName;

    public EnforcementPendingApplicationNotificationTemplateData(final String gobAccountNumber,
                                                                 final int divisionCode,
                                                                 final String caseReference,
                                                                 final LocalDate dateApplicationIsListed,
                                                                 final String defendantName) {
        this.gobAccountNumber = gobAccountNumber;
        this.divisionCode = divisionCode;
        this.caseReference = caseReference;
        this.dateApplicationIsListed = dateApplicationIsListed;
        this.defendantName = defendantName;
    }

    public String getGobAccountNumber() {
        return gobAccountNumber;
    }

    public int getDivisionCode() {
        return divisionCode;
    }

    public String getCaseReference() {
        return caseReference;
    }

    public LocalDate getDateApplicationIsListed() {
        return dateApplicationIsListed;
    }

    public String getDefendantName() {
        return defendantName;
    }

    @Override
    public boolean equals(final Object o) {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
