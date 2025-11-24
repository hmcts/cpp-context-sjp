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
    private final String title;
    private final String defendantAddress;
    private final String defendantDateOfBirth;
    private final String defendantEmail;
    private final String originalDateOfSentence;
    private final String defendantContactNumber;
    private final String courtCentreName;

    public EnforcementPendingApplicationNotificationTemplateData(final String gobAccountNumber,
                                                                 final int divisionCode,
                                                                 final String caseReference,
                                                                 final LocalDate dateApplicationIsListed,
                                                                 final String defendantName,
                                                                 final String title,
                                                                 final String defendantAddress,
                                                                 final String defendantDateOfBirth,
                                                                 final String defendantEmail,
                                                                 final String originalDateOfSentence,
                                                                 final String defendantContactNumber,
                                                                 final String courtCentreName) {
        this.gobAccountNumber = gobAccountNumber;
        this.divisionCode = divisionCode;
        this.caseReference = caseReference;
        this.dateApplicationIsListed = dateApplicationIsListed;
        this.defendantName = defendantName;
        this.title = title;
        this.defendantAddress = defendantAddress;
        this.defendantDateOfBirth = defendantDateOfBirth;
        this.defendantEmail = defendantEmail;
        this.originalDateOfSentence = originalDateOfSentence;
        this.defendantContactNumber = defendantContactNumber;
        this.courtCentreName = courtCentreName;
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

    public String getTitle() { return title; }

    public String getDefendantAddress() {return defendantAddress;}

    public String getDefendantDateOfBirth() {return defendantDateOfBirth;}

    public String getDefendantEmail() {return defendantEmail;}

    public String getOriginalDateOfSentence() {return originalDateOfSentence;}

    public String getDefendantContactNumber() {return defendantContactNumber;}

    public String getCourtCentreName() {return courtCentreName;}

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
