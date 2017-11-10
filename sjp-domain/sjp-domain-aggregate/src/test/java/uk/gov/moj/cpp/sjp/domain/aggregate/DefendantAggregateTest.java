package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static uk.gov.moj.cpp.sjp.domain.aggregate.DefendantAggregateTest.DefendantData.defaultDefendantData;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.PersonInfoDetails;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

public class DefendantAggregateTest {

    private DefendantAggregate defendantAggregate = new DefendantAggregate();

    private static final UUID id = UUID.randomUUID();
    private static final UUID caseId = UUID.randomUUID();
    private static final UUID personId = UUID.randomUUID();
    private static final UUID defendantId = UUID.randomUUID();
    private static final String gender = "M";
    private static final String firstName = "Random";
    private static final String lastName = "Guy";
    private static final String nationalInsuranceNumber = "valid nino";
    private static final LocalDate dateOfBirth = LocalDates.from("2000-01-01");
    private static final String email = "test@example.com";
    private static final String homeNumber = "10";
    private static final String mobileNumber = "number";
    private static final String title = "Mr";
    private static final Address address = new Address("address1", "address2",
            "address3", "address4", "postCode");


    @Test
    public void updatesToValidTitle() {
        updatesToValidTitle("Mr");
        updatesToValidTitle("Mrs");
        updatesToValidTitle("Ms");
        updatesToValidTitle("Miss");
        updatesToValidTitle("Co");
    }

    private void updatesToValidTitle(String validTitle) {
        givenDefaultPersonInfoWasAdded();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewTitle(validTitle)
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(0);
        assertThat(defendantDetailsUpdated.getTitle(), is(validTitle));
    }

    @Test
    public void rejectsNullTitleIfPreviouslySet() {
        givenDefaultPersonInfoWasAdded();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewTitle(null)
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("title parameter can not be null as previous value is : Mr"));
    }

    @Test
    public void rejectsEmptyTitleIfPreviouslySet() {
        givenDefaultPersonInfoWasAdded();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewTitle(" ")
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("title parameter can not be null as previous value is : Mr"));
    }

    @Test
    public void acceptsEmptyTitleIfPreviouslyNotSet() {
        givenDefendantPersonInfoWasAdded(defaultDefendantData().withNewTitle(" "));

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewTitle("")
        );

        assertThat(events.size(), is(1));
        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(0);
        assertThat(defendantDetailsUpdated.getTitle(), is(""));
    }

    @Test
    public void acceptsNewFirstName() {
        givenNoPersonInfoWasAdded();

        final String newFirstName = "Newname";
        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewFirstName(newFirstName)
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(0);
        assertThat(defendantDetailsUpdated.getFirstName(), is(newFirstName));
    }

    @Test
    public void acceptsNewLastName() {
        givenNoPersonInfoWasAdded();

        final String newLastName = "Newname";
        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewLastName(newLastName)
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(0);
        assertThat(defendantDetailsUpdated.getLastName(), is(newLastName));
    }

    @Test
    public void rejectsNullDateOfBirth() {
        givenDefaultPersonInfoWasAdded();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewDateOfBirth(null)
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("dob parameter can not be null"));
    }

    @Test
    public void acceptsNewDateOfBirth() {
        givenDefaultPersonInfoWasAdded();

        final LocalDate newDateOfBirth = LocalDates.from("1990-05-05");
        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewDateOfBirth(newDateOfBirth)
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(0);
        assertThat(defendantDetailsUpdated.getDateOfBirth(), is(newDateOfBirth));
    }

    @Test
    public void rejectsAddressWithMissingStreet() {
        givenDefaultPersonInfoWasAdded();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewAddress(new Address(" ", "address2",
                        "address3", "address4", "postCode"))
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("street (address1) can not be blank as previous value is: address1"));
    }

    @Test
    public void rejectsAddressWithMissingTown() {
        givenDefaultPersonInfoWasAdded();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewAddress(new Address("address1", "address2",
                        "address3", " ", "postCode"))
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("town (address4) can not be blank as previous value is: address4"));
    }

    @Test
    public void rejectsAddressWithMissingPostCode() {
        givenDefaultPersonInfoWasAdded();

        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewAddress(new Address("address1", "address2",
                        "address3", "address4", " "))
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId.toString()));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("postCode can not be blank as previous value is: postCode"));
    }

    @Test
    public void acceptsNewAddress() {
        givenDefaultPersonInfoWasAdded();

        final Address newAddress = new Address("new street", "", "", "new town", "new postcode");
        final List<Object> events = whenTheDefendantIsUpdated(
                defaultDefendantData().withNewAddress(newAddress)
        );

        assertThat(events.size(), is(1));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(0);
        assertThat(defendantDetailsUpdated.getAddress().getAddress1(), is(newAddress.getAddress1()));
        assertThat(defendantDetailsUpdated.getAddress().getAddress2(), is(newAddress.getAddress2()));
        assertThat(defendantDetailsUpdated.getAddress().getAddress3(), is(newAddress.getAddress3()));
        assertThat(defendantDetailsUpdated.getAddress().getAddress4(), is(newAddress.getAddress4()));
        assertThat(defendantDetailsUpdated.getAddress().getPostCode(), is(newAddress.getPostCode()));
    }

    private void givenNoPersonInfoWasAdded() {
    }

    static class DefendantData {
        UUID id = DefendantAggregateTest.id;
        UUID caseId = DefendantAggregateTest.caseId;
        UUID personId = DefendantAggregateTest.personId;
        UUID defendantId = DefendantAggregateTest.defendantId;
        String gender = DefendantAggregateTest.gender;
        String firstName = DefendantAggregateTest.firstName;
        String lastName = DefendantAggregateTest.lastName;
        String nationalInsuranceNumber = DefendantAggregateTest.nationalInsuranceNumber;
        LocalDate dateOfBirth = DefendantAggregateTest.dateOfBirth;
        String email = DefendantAggregateTest.email;
        String homeNumber = DefendantAggregateTest.homeNumber;
        String mobileNumber = DefendantAggregateTest.mobileNumber;
        Address address = DefendantAggregateTest.address;

        String title = DefendantAggregateTest.title;

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

    private void givenDefaultPersonInfoWasAdded() {
        givenDefendantPersonInfoWasAdded(defaultDefendantData());
    }

    private void givenDefendantPersonInfoWasAdded(DefendantData defendantData) {
        PersonInfoDetails personInfoDetails = new PersonInfoDetails(defendantData.personId, defendantData.title,
                defendantData.firstName, defendantData.lastName, defendantData.dateOfBirth, defendantData.address);
        defendantAggregate.addPersonInfo(defendantData.id, defendantData.caseId, personInfoDetails);
    }

    private List<Object> whenTheDefendantIsUpdated(final DefendantData updatedDefendantData) {
        PersonInfoDetails personInfoDetails = new PersonInfoDetails(updatedDefendantData.personId, updatedDefendantData.title,
                updatedDefendantData.firstName, updatedDefendantData.lastName, updatedDefendantData.dateOfBirth, updatedDefendantData.address);

        final Stream<Object> eventStream = defendantAggregate.updateDefendantDetails(updatedDefendantData.caseId,
                updatedDefendantData.defendantId, updatedDefendantData.gender, updatedDefendantData.nationalInsuranceNumber,
                updatedDefendantData.email, updatedDefendantData.homeNumber, updatedDefendantData.mobileNumber,
                personInfoDetails);

        return eventStream.collect(Collectors.toList());
    }
}