package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Defendant;
import uk.gov.moj.cpp.sjp.domain.Interpreter;
import uk.gov.moj.cpp.sjp.domain.Offence;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantPersonalNameUpdated;
import uk.gov.moj.cpp.sjp.event.HearingLanguagePreferenceUpdatedForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterCancelledForDefendant;
import uk.gov.moj.cpp.sjp.event.InterpreterUpdatedForDefendant;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;

public class CaseAggregateDefendantTest {

    private CaseAggregate caseAggregate;

    private final UUID id = UUID.randomUUID();
    private final UUID caseId = UUID.randomUUID();
    private final UUID defendantId = UUID.randomUUID();
    private final Gender gender = Gender.MALE;
    private final String firstName = "Random";
    private final String lastName = "Guy";
    private final String nationalInsuranceNumber = "valid nino";
    private final String driverNumber = "valid_driverNumber";
    private final LocalDate dateOfBirth = LocalDates.from("2000-01-01");
    private final String email = "test_email1@example.com";
    private final String email2 = "test_email2@example.com";
    private final String homeNumber = "10";
    private final String mobileNumber = "mobileNumber";
    private final String businessNumber = "businessNumber";
    private final String title = "Mr";
    private final Address address = new Address("address1", "address2", "address3", "address4", "address5", "CR02FW");
    private final Clock clock = new UtcClock();
    private final String languageNeeds = "languageNeeds_" + RandomStringUtils.randomAlphabetic(10);
    private final ContactDetails contactDetails = new ContactDetails(homeNumber, mobileNumber, businessNumber, email, email2);
    private final int numPreviousConvictions = 0;
    private final List<Offence> offences = emptyList();

    @Before
    public void setUp() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void updatesToValidTitle() {
        givenCaseWasReceivedWithDefaultDefendantData();

        updatesToValidTitle("Mr");
        updatesToValidTitle("Mrs");
        updatesToValidTitle("Ms");
        updatesToValidTitle("Miss");
        updatesToValidTitle("Co");
    }

    private void updatesToValidTitle(String validTitle) {
        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewTitle(validTitle)
        );

        assertThat(events, not(emptyCollectionOf(Object.class)));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(events.size() - 1);
        assertThat(defendantDetailsUpdated.getTitle(), is(validTitle));
    }

    @Test
    public void rejectsNullTitleIfPreviouslySet() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewTitle(null)
        );

        assertThat(events, hasSize(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("title parameter can not be null as previous value is : Mr"));
    }

    @Test
    public void rejectsEmptyTitleIfPreviouslySet() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewTitle(" ")
        );

        assertThat(events, hasSize(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("title parameter can not be null as previous value is : Mr"));
    }

    @Test
    public void acceptsEmptyTitleIfPreviouslyNotSet() {
        givenCaseWasReceivedWithDefendant(new DefendantData().withNewTitle(" "));

        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewTitle("")
        );

        assertThat(events, hasSize(2));
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
                new DefendantData().withNewDateOfBirth(null).withNewFirstName(newFirstName)
        );

        assertThat(events, hasSize(2));

        final DefendantPersonalNameUpdated personalNameUpdated = (DefendantPersonalNameUpdated) events.get(0);
        assertThat(personalNameUpdated.getNewPersonalName().getFirstName(), is(newFirstName));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getFirstName(), is(newFirstName));
    }

    @Test
    public void acceptsNewLastName() {

        final String newLastName = "Newname";
        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewDateOfBirth(null).withNewLastName(newLastName)
        );

        assertThat(events, hasSize(2));

        final DefendantPersonalNameUpdated personalNameUpdated = (DefendantPersonalNameUpdated) events.get(0);
        assertThat(personalNameUpdated.getNewPersonalName().getLastName(), is(newLastName));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getLastName(), is(newLastName));
    }

    @Test
    public void acceptsNewDateOfBirth() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final LocalDate newDateOfBirth = LocalDates.from("1990-05-05");
        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewDateOfBirth(newDateOfBirth)
        );

        assertThat("Event Types: " + events.stream().map(e -> e.getClass().getSimpleName()).collect(toList()),
                events, hasSize(2));

        final DefendantDateOfBirthUpdated defendantDateOfBirthUpdated = (DefendantDateOfBirthUpdated) events.get(0);
        assertThat(defendantDateOfBirthUpdated.getOldDateOfBirth(), is(dateOfBirth));
        assertThat(defendantDateOfBirthUpdated.getNewDateOfBirth(), is(newDateOfBirth));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getDateOfBirth(), is(newDateOfBirth));
    }

    @Test
    public void rejectsAddressWithMissingStreet() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewAddress(new Address(" ", "address2",
                        "address3", "address4", "address5", "CR02FW"))
        );

        assertThat(events, hasSize(1));

        final Object event = events.get(0);
        assertThat(event, instanceOf(DefendantDetailsUpdateFailed.class));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) event;
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("street (address1) can not be blank as previous value is: address1"));
    }

    @Test
    public void rejectsAddressWithMissingTown() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewAddress(new Address("address1", "address2",
                        "", "address4", "", "CR02FW"))
        );

        assertThat(events, hasSize(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("town (address3) can not be blank as previous value is: address3"));
    }

    @Test
    public void acceptsAddressWithMissingAddressLine5() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewAddress(new Address("address1", "address2",
                        "address3", "address4", "", "CR02FW"))
        );

        assertThat(events, hasSize(2));

        final DefendantAddressUpdated defendantAddressUpdated = (DefendantAddressUpdated) events.get(0);
        assertThat(defendantAddressUpdated.getCaseId(), is(caseId));
        assertThat(defendantAddressUpdated.getNewAddress().getAddress5(), is(""));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdated.getAddress().getAddress5(), is(""));
    }

    @Test
    public void rejectsAddressWithMissingPostCode() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewAddress(new Address("address1", "address2",
                        "address3", "address4", "address5", ""))
        );

        assertThat(events, hasSize(1));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) events.get(0);
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString("postcode can not be blank as previous value is: CR02FW"));
    }

    @Test
    public void acceptsNewAddress() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final Address newAddress = new Address("new street", "", "new town", "", "", "CR02FT");
        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewAddress(newAddress)
        );

        assertThat("Event Types: " + events.stream().map(e -> e.getClass().getSimpleName()).collect(toList()),
                events, hasSize(2));

        final DefendantAddressUpdated defendantAddressUpdated = (DefendantAddressUpdated) events.get(0);
        assertThat(defendantAddressUpdated.getNewAddress(), is(newAddress));
        assertThat(defendantAddressUpdated.getOldAddress(), is(address));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getAddress(), is(newAddress));
    }

    @Test
    public void shouldUpdateInterpreterLanguage() {
        final UUID assigneeId = UUID.randomUUID();

        DefendantData defendantData = new DefendantData();
        final CaseReceived caseReceivedEvent = givenCaseWasReceivedWithDefendant(defendantData);

        caseAggregate.assignCase(assigneeId, clock.now(), CaseAssignmentType.MAGISTRATE_DECISION);

        final List<Object> eventsRaised = caseAggregate.updateHearingRequirements(assigneeId, caseReceivedEvent.getDefendant().getId(), "welsh", true)
                .collect(toList());

        InterpreterUpdatedForDefendant interpreterUpdatedEvent = (InterpreterUpdatedForDefendant) eventsRaised.get(0);

        assertThat(interpreterUpdatedEvent.getInterpreter(), is(Interpreter.of("welsh")));
        assertThat(interpreterUpdatedEvent.getCaseId(), is(caseId));
        assertThat(interpreterUpdatedEvent.getDefendantId(), is(caseReceivedEvent.getDefendant().getId()));

        HearingLanguagePreferenceUpdatedForDefendant hearingUpdatedEvent = (HearingLanguagePreferenceUpdatedForDefendant) eventsRaised.get(1);

        assertThat(hearingUpdatedEvent.getSpeakWelsh(), is(true));
    }

    @Test
    public void shouldCancelInterpreterLanguage() {
        final UUID assigneeId = UUID.randomUUID();

        DefendantData defendantData = new DefendantData();
        final CaseReceived caseReceivedEvent = givenCaseWasReceivedWithDefendant(defendantData);

        caseAggregate.assignCase(assigneeId, clock.now(), CaseAssignmentType.MAGISTRATE_DECISION);

        caseAggregate.updateHearingRequirements(assigneeId, caseReceivedEvent.getDefendant().getId(), "welsh", true);
        final List<Object> eventsRaised = caseAggregate.updateHearingRequirements(assigneeId, caseReceivedEvent.getDefendant().getId(), "", false)
                .collect(toList());

        InterpreterCancelledForDefendant interpreterCancelledForDefendant = (InterpreterCancelledForDefendant) eventsRaised.get(0);

        assertThat(interpreterCancelledForDefendant.getCaseId(), is(caseId));
        assertThat(interpreterCancelledForDefendant.getDefendantId(), is(caseReceivedEvent.getDefendant().getId()));
    }

    private void givenCaseWasReceivedWithDefaultDefendantData() {
        givenCaseWasReceivedWithDefendant(new DefendantData());
    }

    private CaseReceived givenCaseWasReceivedWithDefendant(DefendantData defendantData) {
        return (CaseReceived) caseAggregate.receiveCase(
                CaseBuilder.aDefaultSjpCase()
                        .withId(caseId)
                        .withDefendant(new Defendant(
                                defendantData.defendantId,
                                defendantData.title,
                                defendantData.firstName,
                                defendantData.lastName,
                                defendantData.dateOfBirth,
                                defendantData.gender,
                                defendantData.nationalInsuranceNumber,
                                defendantData.driverNumber,
                                defendantData.address,
                                defendantData.contactDetails,
                                defendantData.numPreviousConvictions,
                                defendantData.offences,
                                defendantData.languageNeeds
                        )).build(),
                clock.now()
        ).findFirst().get();
    }

    private List<Object> whenTheDefendantIsUpdated(final DefendantData updatedDefendantData) {
        final Person person = new Person(
                updatedDefendantData.title,
                updatedDefendantData.firstName,
                updatedDefendantData.lastName,
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

    private class DefendantData {
        private UUID caseId = CaseAggregateDefendantTest.this.caseId;
        private UUID defendantId = CaseAggregateDefendantTest.this.defendantId;
        private Gender gender = CaseAggregateDefendantTest.this.gender;
        private String title = CaseAggregateDefendantTest.this.title;
        private String firstName = CaseAggregateDefendantTest.this.firstName;
        private String lastName = CaseAggregateDefendantTest.this.lastName;
        private String nationalInsuranceNumber = CaseAggregateDefendantTest.this.nationalInsuranceNumber;
        private String driverNumber = CaseAggregateDefendantTest.this.driverNumber;
        private LocalDate dateOfBirth = CaseAggregateDefendantTest.this.dateOfBirth;
        private Address address = CaseAggregateDefendantTest.this.address;
        private String languageNeeds = CaseAggregateDefendantTest.this.languageNeeds;
        private ContactDetails contactDetails = CaseAggregateDefendantTest.this.contactDetails;
        private int numPreviousConvictions = CaseAggregateDefendantTest.this.numPreviousConvictions;
        private List<Offence> offences = CaseAggregateDefendantTest.this.offences;

        private DefendantData withNewTitle(final String newTitle) {
            this.title = newTitle;
            return this;
        }

        private DefendantData withNewFirstName(final String newFirstName) {
            this.firstName = newFirstName;
            return this;
        }

        private DefendantData withNewLastName(final String newLastName) {
            this.lastName = newLastName;
            return this;
        }

        private DefendantData withNewDateOfBirth(final LocalDate newDateOfBirth) {
            this.dateOfBirth = newDateOfBirth;
            return this;
        }

        private DefendantData withNewAddress(final Address newAddress) {
            this.address = newAddress;
            return this;
        }

    }

}