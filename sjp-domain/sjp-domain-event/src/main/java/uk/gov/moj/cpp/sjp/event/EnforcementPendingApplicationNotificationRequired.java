package uk.gov.moj.cpp.sjp.event;

import uk.gov.justice.domain.annotation.Event;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Event(EnforcementPendingApplicationNotificationRequired.EVENT_NAME)
public class EnforcementPendingApplicationNotificationRequired {
    public static final String EVENT_NAME = "sjp.events.enforcement-pending-application-notification-required";

    private final UUID caseId;
    private final UUID applicationId;
    private final ZonedDateTime initiatedTime;
    private final String gobAccountNumber;
    private final String defendantName;
    private final String urn;
    private final int divisionCode;
    private final LocalDate dateApplicationIsListed;
    private final String defendantAddress;
    private final String defendantDateOfBirth;
    private final String defendantEmail;
    private final String originalDateOfSentence;
    private final String defendantContactNumber;

    @JsonCreator
    public EnforcementPendingApplicationNotificationRequired(@JsonProperty("caseId") final UUID caseId,
                                                             @JsonProperty("applicationId") final UUID applicationId,
                                                             @JsonProperty("initiatedTime") final ZonedDateTime initiatedTime,
                                                             @JsonProperty("gobAccountNumber") final String gobAccountNumber,
                                                             @JsonProperty("defendantName") final String defendantName,
                                                             @JsonProperty("urn") final String urn,
                                                             @JsonProperty("divisionCode") final int divisionCode,
                                                             @JsonProperty("dateApplicationIsListed") final LocalDate dateApplicationIsListed,
                                                             @JsonProperty("defendantAddress") final String defendantAddress,
                                                             @JsonProperty("defendantDateOfBirth") final String defendantDateOfBirth,
                                                             @JsonProperty("defendantEmail") final String defendantEmail,
                                                             @JsonProperty("originalDateOfSentence") final String originalDateOfSentence,
                                                             @JsonProperty("defendantContactNumber") final String defendantContactNumber) {
        this.caseId = caseId;
        this.applicationId = applicationId;
        this.initiatedTime = initiatedTime;
        this.gobAccountNumber = gobAccountNumber;
        this.defendantName = defendantName;
        this.urn = urn;
        this.divisionCode = divisionCode;
        this.dateApplicationIsListed = dateApplicationIsListed;
        this.defendantEmail = defendantEmail;
        this.defendantAddress = defendantAddress;
        this.defendantDateOfBirth = defendantDateOfBirth;
        this.originalDateOfSentence = originalDateOfSentence;
        this.defendantContactNumber = defendantContactNumber;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public ZonedDateTime getInitiatedTime() {
        return initiatedTime;
    }

    public String getGobAccountNumber() {
        return gobAccountNumber;
    }

    public String getDefendantName() {
        return defendantName;
    }

    public String getUrn() {
        return urn;
    }

    public int getDivisionCode() {
        return divisionCode;
    }

    public LocalDate getDateApplicationIsListed() {
        return dateApplicationIsListed;
    }

    public String getDefendantAddress() {return defendantAddress;}

    public String getDefendantDateOfBirth() {return defendantDateOfBirth;}

    public String getDefendantEmail() {return defendantEmail;}

    public String getOriginalDateOfSentence() {return originalDateOfSentence;}

    public String getDefendantContactNumber() {return defendantContactNumber;}

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnforcementPendingApplicationNotificationRequired)) {
            return false;
        }
        final EnforcementPendingApplicationNotificationRequired that = (EnforcementPendingApplicationNotificationRequired) o;
        final boolean caseIdEquals = Objects.equals(getCaseId(), that.getCaseId());
        final boolean urnEquals = Objects.equals(getUrn(), that.getUrn());
        final boolean defendantNameEquals = Objects.equals(getDefendantName(), that.getDefendantName());
        final boolean gobAccountNumberEquals = Objects.equals(getGobAccountNumber(), that.getGobAccountNumber());
        final boolean initiatedTimeEquals = Objects.equals(getInitiatedTime(), that.getInitiatedTime());
        final boolean applicationIdEquals = Objects.equals(getApplicationId(), that.getApplicationId());
        final boolean dateListedEquals = Objects.equals(getDateApplicationIsListed(), that.getDateApplicationIsListed());

        final boolean equalsCondition1 = defendantNameEquals && urnEquals && dateListedEquals && caseIdEquals;
        final boolean equalsCondition2 = applicationIdEquals && initiatedTimeEquals && gobAccountNumberEquals;
        final boolean divisionCodeEquals = getDivisionCode() == that.getDivisionCode();

        return equalsCondition1 && equalsCondition2 && divisionCodeEquals;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getCaseId(),getApplicationId(), getInitiatedTime(),
                getGobAccountNumber(), getDefendantName(),
                getUrn(), getDivisionCode(), getDateApplicationIsListed());
    }
}
