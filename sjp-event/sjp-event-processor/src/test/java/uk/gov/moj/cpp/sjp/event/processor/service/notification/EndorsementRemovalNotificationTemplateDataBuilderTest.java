package uk.gov.moj.cpp.sjp.event.processor.service.notification;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import uk.gov.justice.json.schemas.domains.sjp.Address;
import uk.gov.justice.json.schemas.domains.sjp.ApplicationType;
import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.PersonalDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

public class EndorsementRemovalNotificationTemplateDataBuilderTest {

    private EndorsementRemovalNotificationTemplateDataBuilder builder = new EndorsementRemovalNotificationTemplateDataBuilder();

    @Test
    public void shouldPopulateDefendantsFullName() {
        final Defendant defendant = Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                .withFirstName("Firstname")
                .withLastName("Lastname")
                .build()).build();

        final EndorsementRemovalNotificationTemplateData templateData = builder.withDefendant(defendant).build();

        assertThat(templateData.getDefendantName(), equalTo("Firstname Lastname"));
    }

    @Test
    public void shouldHandleOnlyDefendantsFirstNamePresent() {
        final Defendant defendant = Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                .withFirstName("Firstname")
                .build()).build();

        final EndorsementRemovalNotificationTemplateData templateData = builder.withDefendant(defendant).build();

        assertThat(templateData.getDefendantName(), equalTo("Firstname"));
    }

    @Test
    public void shouldHandleOnlyDefendantsLastNamePresent() {
        final Defendant defendant = Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                .withLastName("Lastname")
                .build()).build();

        final EndorsementRemovalNotificationTemplateData templateData = builder.withDefendant(defendant).build();

        assertThat(templateData.getDefendantName(), equalTo("Lastname"));
    }

    @Test
    public void shouldHandleNullDefendantNames() {
        final Defendant defendant = Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                .withFirstName(null)
                .withLastName(null)
                .build()).build();

        final EndorsementRemovalNotificationTemplateData templateData = builder.withDefendant(defendant).build();

        assertThat(templateData.getDefendantName(), equalTo(null));
    }

    @Test
    public void shouldPopulateDefendantsAddress() {
        final Defendant defendant = Defendant.defendant().withPersonalDetails(
                PersonalDetails.personalDetails().withAddress(
                        Address.address()
                                .withAddress1("line1")
                                .withAddress2("line2")
                                .withAddress3("line3")
                                .withAddress4("line4")
                                .withAddress5("line5")
                                .withPostcode("Postcode")
                                .build()
                ).build()
        ).build();

        final EndorsementRemovalNotificationTemplateData templateData = builder.withDefendant(defendant).build();

        assertThat(templateData.getDefendantAddress(), equalTo("line1, line2, line3, line4, line5, Postcode"));
    }

    @Test
    public void shouldHandleNullableDefendantsAddressLines() {
        final Defendant defendant = Defendant.defendant().withPersonalDetails(
                PersonalDetails.personalDetails().withAddress(
                        Address.address()
                                .withAddress1("line1")
                                .withAddress4("line4")
                                .withPostcode("Postcode")
                                .build()
                ).build()
        ).build();

        final EndorsementRemovalNotificationTemplateData templateData = builder.withDefendant(defendant).build();

        assertThat(templateData.getDefendantAddress(), equalTo("line1, line4, Postcode"));
    }

    @Test
    public void shouldHandleEmptyDefendantsAddress() {
        final Defendant defendant = Defendant.defendant().withPersonalDetails(
                PersonalDetails.personalDetails().withAddress(
                        Address.address().build()).build()).build();

        final EndorsementRemovalNotificationTemplateData templateData = builder.withDefendant(defendant).build();

        assertThat(templateData.getDefendantAddress(), nullValue());
    }

    @Test
    public void shouldHandleNullDefendantsAddress() {
        final Defendant defendant = Defendant.defendant().withPersonalDetails(
                PersonalDetails.personalDetails().withAddress(null).build()).build();

        final EndorsementRemovalNotificationTemplateData templateData = builder.withDefendant(defendant).build();

        assertThat(templateData.getDefendantAddress(), nullValue());
    }

    @Test
    public void shouldHandleNullDefendantsPersonDetails() {
        final Defendant defendant = Defendant.defendant().withPersonalDetails(null).build();

        final EndorsementRemovalNotificationTemplateData templateData = builder.withDefendant(defendant).build();

        assertThat(templateData.getDefendantName(), nullValue());
        assertThat(templateData.getDefendantAddress(), nullValue());
        assertThat(templateData.getDefendantDateOfBirth(), nullValue());
        assertThat(templateData.getDefendantGender(), nullValue());
        assertThat(templateData.getDefendantDriverNumber(), nullValue());
    }

    @Test
    public void shouldPopulateDefendantsGender() {
        final Defendant femaleDefendant = Defendant.defendant().withPersonalDetails(
                PersonalDetails.personalDetails().withGender(Gender.FEMALE).build()).build();
        final Defendant maleDefendant = Defendant.defendant().withPersonalDetails(
                PersonalDetails.personalDetails().withGender(Gender.MALE).build()).build();
        final Defendant unspecifiedGenderDefendant = Defendant.defendant().withPersonalDetails(
                PersonalDetails.personalDetails().withGender(Gender.NOT_SPECIFIED).build()).build();

        final EndorsementRemovalNotificationTemplateData templateDataForFemale = builder.withDefendant(femaleDefendant).build();
        final EndorsementRemovalNotificationTemplateData templateDataForMale = builder.withDefendant(maleDefendant).build();
        final EndorsementRemovalNotificationTemplateData templateDataForUnspecifiedGender = builder.withDefendant(unspecifiedGenderDefendant).build();

        assertThat(templateDataForFemale.getDefendantGender(), equalTo("Female"));
        assertThat(templateDataForMale.getDefendantGender(), equalTo("Male"));
        assertThat(templateDataForUnspecifiedGender.getDefendantGender(), equalTo("Not Specified"));
    }

    @Test
    public void shouldHandleNullDefendantDateOfBirth() {
        final Defendant defendant = Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                .withDateOfBirth(null)
                .build()).build();

        final EndorsementRemovalNotificationTemplateData templateData = builder.withDefendant(defendant).build();

        assertThat(templateData.getDefendantDateOfBirth(), equalTo("unknown"));
    }

    @Test
    public void shouldFormatDefendantDateOfBirthHavingDayWihSingleAndDoubleDigits() {
        final Defendant.Builder defendantWithSingleDigitDateOfBirthDay = Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                .withDateOfBirth(LocalDate.of(1980, 5, 7))
                .build());
        assertThat(builder.withDefendant(defendantWithSingleDigitDateOfBirthDay.build()).build().getDefendantDateOfBirth(), equalTo("7 May 1980"));

        final Defendant defendantWithDoubleDigitDateOfBirthDay = Defendant.defendant().withPersonalDetails(PersonalDetails.personalDetails()
                .withDateOfBirth(LocalDate.of(1981, 12, 13))
                .build()).build();
        assertThat(builder.withDefendant(defendantWithDoubleDigitDateOfBirthDay).build().getDefendantDateOfBirth(), equalTo("13 December 1981"));
    }

    @Test
    public void shouldHandlerNullDefendantDriverNumber() {
        final Defendant defendant = Defendant.defendant()
                .withPersonalDetails(PersonalDetails.personalDetails()
                        .withDriverNumber(null)
                        .build()).build();

        final EndorsementRemovalNotificationTemplateData templateData = builder.withDefendant(defendant).build();

        assertThat(templateData.getDefendantDriverNumber(), nullValue());
    }

    @Test
    public void shouldFormatDateOfOrder() {
        assertThat(builder.withDateOfOrder(LocalDate.of(2020, 3, 5)).build().getDateOfOrder(), equalTo("5 March 2020"));
        assertThat(builder.withDateOfOrder(LocalDate.of(2020, 11, 21)).build().getDateOfOrder(), equalTo("21 November 2020"));
    }

    @Test
    public void shouldSetReasonForIssue() {
        final EndorsementRemovalNotificationTemplateData templateDataStatDecs = builder.withReasonForIssue(ApplicationType.STAT_DEC).build();
        assertThat(templateDataStatDecs.getReasonForIssue(), equalTo("Statutory declaration accepted"));

        final EndorsementRemovalNotificationTemplateData templateDataReopening = builder.withReasonForIssue(ApplicationType.REOPENING).build();
        assertThat(templateDataReopening.getReasonForIssue(), equalTo("Case re-opened under section 142"));
    }

    @Test
    public void shouldHandleNullReasonForIssue() {
        final EndorsementRemovalNotificationTemplateData templateData = builder.withReasonForIssue(null).build();
        assertThat(templateData.getReasonForIssue(), nullValue());
    }
}