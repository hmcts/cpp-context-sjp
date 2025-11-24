package uk.gov.moj.cpp.sjp.event.processor.service.notification;

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.trim;
import static uk.gov.justice.json.schemas.domains.sjp.ApplicationType.STAT_DEC;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

public class EndorsementRemovalNotificationTemplateDataBuilder {

    private String dateOfOrder;
    private String ljaCode;
    private String ljaName;
    private String caseUrn;
    private String defendantName;
    private String defendantAddress;
    private String defendantDateOfBirth;
    private String defendantGender;
    private String defendantDriverNumber;
    private String reasonForIssue;
    private List<DrivingEndorsementToBeRemoved> drivingEndorsementsToBeRemoved;

    public static String formatDate(final LocalDate date) {
        return date.format(DateTimeFormatter.ofPattern("d MMMM YYYY"));
    }

    public EndorsementRemovalNotificationTemplateDataBuilder withDateOfOrder(final LocalDate dateOfOrder) {
        this.dateOfOrder = formatDate(dateOfOrder);
        return this;
    }

    public EndorsementRemovalNotificationTemplateDataBuilder withLjaCode(final String ljaCode) {
        this.ljaCode = ljaCode;
        return this;
    }

    public EndorsementRemovalNotificationTemplateDataBuilder withLjaName(final String ljaName) {
        this.ljaName = ljaName;
        return this;
    }

    public EndorsementRemovalNotificationTemplateDataBuilder withCaseUrn(final String caseUrn) {
        this.caseUrn = caseUrn;
        return this;
    }

    public EndorsementRemovalNotificationTemplateDataBuilder withDefendant(final Defendant defendant) {
        if (nonNull(defendant.getPersonalDetails())) {
            setDefendantName(defendant.getPersonalDetails());
            setDefendantAddress(defendant.getPersonalDetails());
            setDefendantGender(defendant.getPersonalDetails());
            setDefendantDriverNumber(defendant.getPersonalDetails());
            setDefendantDateOfBirth(defendant.getPersonalDetails());
        }
        return this;
    }

    public EndorsementRemovalNotificationTemplateDataBuilder withDefendantName(final String defendantName) {
        this.defendantName = defendantName;
        return this;
    }

    public EndorsementRemovalNotificationTemplateDataBuilder withDefendantAddress(final String defendantAddress) {
        this.defendantAddress = defendantAddress;
        return this;
    }

    public EndorsementRemovalNotificationTemplateDataBuilder withDefendantGender(final String defendantGender) {
        this.defendantGender = defendantGender;
        return this;
    }

    public EndorsementRemovalNotificationTemplateDataBuilder withDefendantDriverNumber(final String defendantDriverNumber) {
        this.defendantDriverNumber = defendantDriverNumber;
        return this;
    }

    public EndorsementRemovalNotificationTemplateDataBuilder withReasonForIssue(final ApplicationType reasonForIssue) {
        if (nonNull(reasonForIssue)) {
            this.reasonForIssue = reasonForIssue == STAT_DEC ? "Statutory declaration accepted" : "Case re-opened under section 142";
        }
        return this;
    }

    public EndorsementRemovalNotificationTemplateDataBuilder withDrivingEndorsementsToBeRemoved(final List<DrivingEndorsementToBeRemoved> drivingEndorsementsToBeRemoved) {
        this.drivingEndorsementsToBeRemoved = drivingEndorsementsToBeRemoved;
        return this;
    }

    public EndorsementRemovalNotificationTemplateData build() {
        return new EndorsementRemovalNotificationTemplateData(
                this.dateOfOrder,
                this.ljaCode,
                this.ljaName,
                this.caseUrn,
                this.defendantName,
                this.defendantAddress,
                this.defendantDateOfBirth,
                this.defendantGender,
                this.defendantDriverNumber,
                this.reasonForIssue,
                this.drivingEndorsementsToBeRemoved
        );
    }

    private void setDefendantName(final PersonalDetails personalDetails) {
        final String firstName = ofNullable(personalDetails.getFirstName()).orElse("");
        final String lastName = ofNullable(personalDetails.getLastName()).orElse("");
        final String fullName = trim(firstName + " " + lastName);
        this.defendantName = isNotEmpty(fullName) ? fullName : null;
    }

    private void setDefendantAddress(final PersonalDetails personalDetails) {
        if (nonNull(personalDetails.getAddress())) {
            final Address address = personalDetails.getAddress();
            final String fullAddress = Stream.of(
                    address.getAddress1(),
                    address.getAddress2(),
                    address.getAddress3(),
                    address.getAddress4(),
                    address.getAddress5(),
                    address.getPostcode()
            )
                    .filter(StringUtils::isNotEmpty)
                    .collect(Collectors.joining(", ")).trim();

            this.defendantAddress = isNotEmpty(fullAddress) ? fullAddress : null;
        }
    }

    private void setDefendantGender(final PersonalDetails personalDetails) {
        if (nonNull(personalDetails.getGender())) {
            this.defendantGender = personalDetails.getGender().toString();
        }
    }

    private void setDefendantDriverNumber(final PersonalDetails personalDetails) {
        if (StringUtils.isNotEmpty(personalDetails.getDriverNumber())) {
            this.defendantDriverNumber = personalDetails.getDriverNumber();
        }
    }

    private void setDefendantDateOfBirth(final PersonalDetails personalDetails) {
        this.defendantDateOfBirth = ofNullable(personalDetails.getDateOfBirth())
                .map(EndorsementRemovalNotificationTemplateDataBuilder::formatDate)
                .orElse("unknown");
    }
}
