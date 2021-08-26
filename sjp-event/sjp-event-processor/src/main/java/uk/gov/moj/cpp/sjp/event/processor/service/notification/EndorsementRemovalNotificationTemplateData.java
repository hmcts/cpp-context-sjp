package uk.gov.moj.cpp.sjp.event.processor.service.notification;

import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class EndorsementRemovalNotificationTemplateData {

    private final String dateOfOrder;
    private final String ljaCode;
    private final String ljaName;
    private final String caseUrn;
    private final String defendantName;
    private final String defendantAddress;
    private final String defendantDateOfBirth;
    private final String defendantGender;
    private final String defendantDriverNumber;
    private final String reasonForIssue;
    private final List<DrivingEndorsementToBeRemoved> drivingEndorsementsToBeRemoved;

    public EndorsementRemovalNotificationTemplateData(final String dateOfOrder,
                                                      final String ljaCode,
                                                      final String ljaName,
                                                      final String caseUrn,
                                                      final String defendantName,
                                                      final String defendantAddress,
                                                      final String defendantDateOfBirth,
                                                      final String defendantGender,
                                                      final String defendantDriverNumber,
                                                      final String reasonForIssue,
                                                      final List<DrivingEndorsementToBeRemoved> drivingEndorsementsToBeRemoved) {
        this.dateOfOrder = dateOfOrder;
        this.ljaCode = ljaCode;
        this.ljaName = ljaName;
        this.caseUrn = caseUrn;
        this.defendantName = defendantName;
        this.defendantAddress = defendantAddress;
        this.defendantDateOfBirth = defendantDateOfBirth;
        this.defendantGender = defendantGender;
        this.defendantDriverNumber = defendantDriverNumber;
        this.reasonForIssue = reasonForIssue;
        this.drivingEndorsementsToBeRemoved = drivingEndorsementsToBeRemoved;
    }

    public String getDateOfOrder() {
        return dateOfOrder;
    }

    public String getLjaCode() {
        return ljaCode;
    }

    public String getLjaName() {
        return ljaName;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public String getDefendantAddress() {
        return defendantAddress;
    }

    public String getDefendantDateOfBirth() {
        return defendantDateOfBirth;
    }

    public String getDefendantGender() {
        return defendantGender;
    }

    public String getDefendantDriverNumber() {
        return defendantDriverNumber;
    }

    public String getReasonForIssue() {
        return reasonForIssue;
    }

    public List<DrivingEndorsementToBeRemoved> getDrivingEndorsementsToBeRemoved() {
        return drivingEndorsementsToBeRemoved;
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
