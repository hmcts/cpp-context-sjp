package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateDefendantTest.DefendantData.defaultDefendantData;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Language;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

public class CaseAggregateDefendantTest {

    private CaseAggregate caseAggregate = new CaseAggregate();

    private static final UUID id = UUID.randomUUID();
    private static final UUID caseId = UUID.randomUUID();
    private static final UUID defendantId = UUID.randomUUID();
    private static final String gender = "M";
    private static final String firstName = "Random";
    private static final String lastName = "Guy";
    private static final String forename2 = "forename2_" + RandomStringUtils.randomAlphabetic(10);
    private static final String forename3 = "forename3_" + RandomStringUtils.randomAlphabetic(10);
    private static final String nationalInsuranceNumber = "valid nino";
    private static final String driverNumber = "valid_driverNumber";
    private static final LocalDate dateOfBirth = LocalDates.from("2000-01-01");
    private static final String email = "test_email1@example.com";
    private static final String email2 = "test_email2@example.com";
    private static final String homeNumber = "10";
    private static final String mobileNumber = "mobileNumber";
    private static final String businessNumber = "businessNumber";
    private static final String title = "Mr";
    private static final Address address = new Address("address1", "address2", "address3", "address4", "address5","CR02FW");
    private static final Clock clock = new UtcClock();
    private static final Language documentationLanguage = Language.WELSH;
    private static final Language hearingLanguageIndicator = Language.ENGLISH;
    private static final String languageNeeds = "languageNeeds_" + RandomStringUtils.randomAlphabetic(10);
    private static final ContactDetails contactDetails = new ContactDetails(homeNumber, mobileNumber, businessNumber, email, email2);

    @Test
    public void updatesToValidTitle() {
        updatesToValidTitle("Mr");
        updatesToValidTitle("Mrs");
        updatesToValidTitle("Ms");
        updatesToValidTitle("Miss");
        updatesToValidTitle("Co");
    }

    private void updatesToValidTitle(String validTitle) {
        givenCaseWasReceivedWithDetaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewTitle(validTitle)
        );

        assertThat(events.size(), greaterThan(0));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(events.size() - 1);
        assertThat(defendantDetailsUpdated.getTitle(), is(validTitle));
    }

    @Test
    public void rejectsNullTitleIfPreviouslySet() {
        givenCaseWasReceivedWithDetaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewTitle(null)
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("title parameter can not be null as previous value is : Mr"));
    }

    @Test
    public void rejectsEmptyTitleIfPreviouslySet() {
        givenCaseWasReceivedWithDetaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewTitle(" ")
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("title parameter can not be null as previous value is : Mr"));
    }

    @Test
    public void acceptsEmptyTitleIfPreviouslyNotSet() {
        givenCaseWasReceivedWithDefendant(defaultDefendantData().withNewTitle(" "));

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewTitle("")
        );

        assertThat(events.size(), is(2));
        final DefendantPersonalNameUpdated defendantPersonalNameUpdated = (DefendantPersonalNameUpdated) events.get(0);
        assertThat(defendantPersonalNameUpdated.getOldPersonalName().getTitle(), is(" "));
        assertThat(defendantPersonalNameUpdated.getNewPersonalName().getTitle(), is(""));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getTitle(), is(""));
    }

    @Test
    public void acceptsNewFirstName() {

        final String newFirstName = "Newname";
        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewDateOfBirth(null).withNewFirstName(newFirstName)
        );

        assertThat(events.size(), is(2));

        final DefendantPersonalNameUpdated personalNameUpdated = (DefendantPersonalNameUpdated) events.get(0);
        assertThat(personalNameUpdated.getNewPersonalName().getFirstName(), is(newFirstName));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getFirstName(), is(newFirstName));
    }

    @Test
    public void acceptsNewLastName() {

        final String newLastName = "Newname";
        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewDateOfBirth(null).withNewLastName(newLastName)
        );

        assertThat(events.size(), is(2));

        final DefendantPersonalNameUpdated personalNameUpdated = (DefendantPersonalNameUpdated) events.get(0);
        assertThat(personalNameUpdated.getNewPersonalName().getLastName(), is(newLastName));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getLastName(), is(newLastName));
    }

    @Test
    public void acceptsNewDateOfBirth() {
        givenCaseWasReceivedWithDetaultDefendantData();

        final LocalDate newDateOfBirth = LocalDates.from("1990-05-05");
        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewDateOfBirth(newDateOfBirth)
        );

        assertThat("Event Types: " + events.stream().map(e -> e.getClass().getSimpleName()).collect(toList()),
                events.size(),
                is(2));

        final DefendantDateOfBirthUpdated defendantDateOfBirthUpdated = (DefendantDateOfBirthUpdated) events.get(0);
        assertThat(defendantDateOfBirthUpdated.getOldDateOfBirth(), is(dateOfBirth));
        assertThat(defendantDateOfBirthUpdated.getNewDateOfBirth(), is(newDateOfBirth));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getDateOfBirth(), is(newDateOfBirth));
    }

    @Test
    public void rejectsAddressWithMissingStreet() {
        givenCaseWasReceivedWithDetaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewAddress(new Address(" ", "address2",
                        "address3", "address4", "CR02FW"))
        );

        assertThat(events.size(), is(1));

        final Object event = events.get(0);
        assertThat(event, instanceOf(DefendantDetailsUpdateFailed.class));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) event;
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("street (address1) can not be blank as previous value is: address1"));
    }

    @Test
    public void rejectsAddressWithMissingTown() {
        givenCaseWasReceivedWithDetaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewAddress(new Address("address1", "address2",
                        "address3", " ", "CR02FW"))
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("town (address4) can not be blank as previous value is: address4"));
    }

    @Test
    public void rejectsAddressWithMissingPostCode() {
        givenCaseWasReceivedWithDetaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewAddress(new Address("address1", "address2",
                        "address3", "address4", " "))
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("postcode can not be blank as previous value is: CR02FW"));
    }

    @Test
    public void acceptsNewAddress() {
        givenCaseWasReceivedWithDetaultDefendantData();

        final Address newAddress = new Address("new street", "", "", "new town", "CR02FT");
        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewAddress(newAddress)
        );

        assertThat("Event Types: " + events.stream().map(e -> e.getClass().getSimpleName()).collect(toList()),
                events.size(),
                is(2));

        final DefendantAddressUpdated defendantAddressUpdated = (DefendantAddressUpdated) events.get(0);
        assertThat(defendantAddressUpdated.getNewAddress(), is(newAddress));
        assertThat(defendantAddressUpdated.getOldAddress(), is(address));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getAddress(), is(newAddress));
    }

    static class DefendantData {
        UUID id = CaseAggregateDefendantTest.id;
        UUID caseId = CaseAggregateDefendantTest.caseId;
        UUID defendantId = CaseAggregateDefendantTest.defendantId;
        String gender = CaseAggregateDefendantTest.gender;
        String title = CaseAggregateDefendantTest.title;
        String firstName = CaseAggregateDefendantTest.firstName;
        String lastName = CaseAggregateDefendantTest.lastName;
        String forename2 = CaseAggregateDefendantTest.forename2;
        String forename3 = CaseAggregateDefendantTest.forename3;
        String nationalInsuranceNumber = CaseAggregateDefendantTest.nationalInsuranceNumber;
        String driverNumber = CaseAggregateDefendantTest.driverNumber;
        LocalDate dateOfBirth = CaseAggregateDefendantTest.dateOfBirth;
        Address address = CaseAggregateDefendantTest.address;
        Language documentationLanguage = CaseAggregateDefendantTest.documentationLanguage;
        Language hearingLanguageIndicator = CaseAggregateDefendantTest.hearingLanguageIndicator;
        String languageNeeds = CaseAggregateDefendantTest.languageNeeds;
        ContactDetails contactDetails = CaseAggregateDefendantTest.contactDetails;

        DefendantData withNewTitle(final String newTitle) {
            this.title = newTitle;
            return this;
        }

        DefendantData withNewFirstName(final String newFirstName) {
            this.firstName = newFirstName;
            return this;
        }

        DefendantData withNewLastName(final String newLastName) {
            this.lastName = newLastName;
            return this;
        }

        DefendantData withNewDateOfBirth(final LocalDate newDateOfBirth) {
            this.dateOfBirth = newDateOfBirth;
            return this;
        }

        DefendantData withNewAddress(final Address newAddress) {
            this.address = newAddress;
            return this;
        }

        static DefendantData defaultDefendantData() {
            return new DefendantData();
        }
    }

    private void givenCaseWasReceivedWithDetaultDefendantData() {
        givenCaseWasReceivedWithDefendant(defaultDefendantData());
    }

    private void givenCaseWasReceivedWithDefendant(DefendantData defendantData) {
        caseAggregate.receiveCase(
                CaseBuilder.aDefaultSjpCase().withDefendant(new Defendant(
                        defendantData.id,
                        defendantData.title,
                        defendantData.firstName,
                        defendantData.lastName,
                        defendantData.forename2,
                        defendantData.forename3,
                        defendantData.dateOfBirth,
                        defendantData.gender,
                        defendantData.nationalInsuranceNumber,
                        defendantData.driverNumber,
                        defendantData.address,
                        defendantData.contactDetails,
                        0,
                        emptyList(),
                        defendantData.documentationLanguage,
                        defendantData.hearingLanguageIndicator,
                        defendantData.languageNeeds
                )).build(),
                clock.now()
        );
    }

    private List<Object> whenTheDefendantIsUpdated(final DefendantData updatedDefendantData) {
        final Person person = new Person(
                updatedDefendantData.title,
                updatedDefendantData.firstName,
                updatedDefendantData.lastName,
                updatedDefendantData.forename2,
                updatedDefendantData.forename3,
                updatedDefendantData.dateOfBirth,
                updatedDefendantData.gender,
                updatedDefendantData.nationalInsuranceNumber,
                updatedDefendantData.driverNumber,
                updatedDefendantData.address,
                updatedDefendantData.contactDetails);

        final Stream<Object> eventStream = caseAggregate.updateDefendantDetails(
                updatedDefendantData.caseId,
                updatedDefendantData.defendantId,
                person,
                clock.now());

        return eventStream.collect(toList());
    }
}