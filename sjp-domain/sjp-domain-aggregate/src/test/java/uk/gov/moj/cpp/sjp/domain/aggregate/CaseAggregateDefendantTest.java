package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.StringContains.containsString;

import uk.gov.justice.json.schemas.domains.sjp.Gender;
import uk.gov.justice.json.schemas.domains.sjp.Language;
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
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
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

    private final UUID caseId = UUID.randomUUID();
    private final UUID defendantId = UUID.randomUUID();
    private final Gender gender = Gender.MALE;
    private final String firstName = "Random";
    private final String lastName = "Guy";
    private final String nationalInsuranceNumber = "valid nino";
    private final String driverNumber = "TESTY708166G99KZ";
    private final String driverLicenceDetails = "driver_licence_details";
    private final LocalDate dateOfBirth = LocalDates.from("2000-01-01");
    private final String email = "test_email1@example.com";
    private final String email2 = "test_email2@example.com";
    private final String homeNumber = "10";
    private final String mobileNumber = "mobileNumber";
    private final String businessNumber = "businessNumber";
    private final String title = "Mr";
    private final Address address = new Address("address1", "address2", "address3", "address4", "address5", "CR02FW");
    private final Clock clock = new UtcClock();
    private final Language hearingLanguage = Language.E;
    private final String languageNeeds = "languageNeeds_" + RandomStringUtils.randomAlphabetic(10);
    private final ContactDetails contactDetails = new ContactDetails(homeNumber, mobileNumber, businessNumber, email, email2);
    private final int numPreviousConvictions = 0;
    private final List<Offence> offences = emptyList();
    private CaseAggregate caseAggregate;
    private UUID userId;

    @Before
    public void setUp() {
        caseAggregate = new CaseAggregate();
    }

    @Test
    public void updatesToValidTitle() {
        userId = randomUUID();
        givenCaseWasReceivedWithDefaultDefendantData();

        updatesToValidTitle("Mr");
        updatesToValidTitle("Mrs");
        updatesToValidTitle("Ms");
        updatesToValidTitle("Miss");
        updatesToValidTitle("Co");
    }

    private void updatesToValidTitle(final String validTitle) {
        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewTitle(validTitle)
        );

        assertThat(events, not(emptyCollectionOf(Object.class)));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(events.size() - 1);
        assertThat(defendantDetailsUpdated.getTitle(), is(validTitle));
    }

    @Test
    public void allowsNullTitleIfPreviouslySet() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewTitle(null)
        );
        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(events.size() - 1);
        assertThat(defendantDetailsUpdated.getTitle(), isEmptyOrNullString());
    }

    @Test
    public void rejectsEmptyTitleIfPreviouslySet() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewTitle(" ")
        );

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(events.size() - 1);
        assertThat(defendantDetailsUpdated.getTitle(), is(" "));
    }

    @Test
    public void allowsEmptyTitleIfPreviouslyNotSet() {
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
        givenCaseWasReceivedWithDefaultDefendantData();
        final String newFirstName = "Newname";
        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewFirstName(newFirstName)
        );

        assertThat(events, hasSize(2));

        final DefendantPersonalNameUpdated personalNameUpdated = (DefendantPersonalNameUpdated) events.get(0);
        assertThat(personalNameUpdated.getNewPersonalName().getFirstName(), is(newFirstName));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getFirstName(), is(newFirstName));
    }

    @Test
    public void acceptsNewLastName() {
        givenCaseWasReceivedWithDefaultDefendantData();
        final String newLastName = "Newname";
        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewLastName(newLastName)
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

        doRejectAssertions(events, "street (address1) can not be blank as previous value is: address1");

    }

    @Test
    public void rejectsAddressWithMissingTown() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewAddress(new Address("address1", "address2",
                        "", "address4", "", "CR02FW"))
        );

        doRejectAssertions(events, "town (address3) can not be blank as previous value is: address3");
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
    public void acceptsAddressWithMissingPostCode() {
        givenCaseWasReceivedWithDefaultDefendantData();

        final List<Object> events = whenTheDefendantIsUpdated(
                new DefendantData().withNewAddress(new Address("address1", "address2",
                        "address3", "address4", "address5", ""))
        );

        assertThat(events, hasSize(2));

        final DefendantAddressUpdated defendantAddressUpdated = (DefendantAddressUpdated) events.get(0);
        assertThat(defendantAddressUpdated.getNewAddress().getPostcode(), is(""));

        final DefendantDetailsUpdated defendantDetailsUpdated = (DefendantDetailsUpdated) events.get(1);
        assertThat(defendantDetailsUpdated.getAddress().getPostcode(), is(""));


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

        final DefendantData defendantData = new DefendantData();
        final CaseReceived caseReceivedEvent = givenCaseWasReceivedWithDefendant(defendantData);

        caseAggregate.assignCase(assigneeId, clock.now(), CaseAssignmentType.MAGISTRATE_DECISION);

        final List<Object> eventsRaised = caseAggregate.updateHearingRequirements(assigneeId, caseReceivedEvent.getDefendant().getId(), "welsh", true, PleaMethod.ONLINE, clock.now())
                .collect(toList());

        final InterpreterUpdatedForDefendant interpreterUpdatedEvent = (InterpreterUpdatedForDefendant) eventsRaised.get(0);

        assertThat(interpreterUpdatedEvent.getInterpreter(), is(Interpreter.of("welsh")));
        assertThat(interpreterUpdatedEvent.getCaseId(), is(caseId));
        assertThat(interpreterUpdatedEvent.getDefendantId(), is(caseReceivedEvent.getDefendant().getId()));

        final HearingLanguagePreferenceUpdatedForDefendant hearingUpdatedEvent = (HearingLanguagePreferenceUpdatedForDefendant) eventsRaised.get(1);

        assertThat(hearingUpdatedEvent.getSpeakWelsh(), is(true));
    }

    @Test
    public void shouldCancelInterpreterLanguage() {
        final UUID assigneeId = UUID.randomUUID();

        final DefendantData defendantData = new DefendantData();
        final CaseReceived caseReceivedEvent = givenCaseWasReceivedWithDefendant(defendantData);

        caseAggregate.assignCase(assigneeId, clock.now(), CaseAssignmentType.MAGISTRATE_DECISION);

        caseAggregate.updateHearingRequirements(assigneeId, caseReceivedEvent.getDefendant().getId(), "welsh", true, PleaMethod.ONLINE, clock.now());
        final List<Object> eventsRaised = caseAggregate.updateHearingRequirements(assigneeId, caseReceivedEvent.getDefendant().getId(), "", false, PleaMethod.ONLINE, clock.now())
                .collect(toList());

        final InterpreterCancelledForDefendant interpreterCancelledForDefendant = (InterpreterCancelledForDefendant) eventsRaised.get(0);

        assertThat(interpreterCancelledForDefendant.getCaseId(), is(caseId));
        assertThat(interpreterCancelledForDefendant.getDefendantId(), is(caseReceivedEvent.getDefendant().getId()));
    }

    private void doRejectAssertions(final List<Object> events, final String descriptionContains) {
        assertThat(events, hasSize(1));

        final Object event = events.get(0);
        assertThat(event, instanceOf(DefendantDetailsUpdateFailed.class));

        final DefendantDetailsUpdateFailed defendantDetailsUpdateFailed = (DefendantDetailsUpdateFailed) event;
        assertThat(defendantDetailsUpdateFailed.getCaseId(), is(caseId));
        assertThat(defendantDetailsUpdateFailed.getDefendantId(), is(defendantId));
        assertThat(defendantDetailsUpdateFailed.getDescription(), containsString(descriptionContains));
    }

    private void givenCaseWasReceivedWithDefaultDefendantData() {
        givenCaseWasReceivedWithDefendant(new DefendantData());
    }

    private CaseReceived givenCaseWasReceivedWithDefendant(final DefendantData defendantData) {
        return (CaseReceived) caseAggregate.receiveCase(
                CaseBuilder.aDefaultSjpCase()
                        .withId(caseId)
                        .withDefendant( new Defendant(
                                defendantData.defendantId,
                                defendantData.title,
                                defendantData.firstName,
                                defendantData.lastName,
                                defendantData.dateOfBirth,
                                defendantData.gender,
                                defendantData.nationalInsuranceNumber,
                                defendantData.driverNumber,
                                defendantData.driverLicenceDetails,
                                defendantData.address,
                                defendantData.contactDetails,
                                defendantData.numPreviousConvictions,
                                defendantData.offences,
                                defendantData.hearingLanguage,
                                defendantData.languageNeeds,
                                defendantData.region,
                                defendantData.asn,
                                defendantData.pncIdentifier
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
                updatedDefendantData.driverLicenceDetails,
                updatedDefendantData.address,
                updatedDefendantData.contactDetails,
                updatedDefendantData.region);

        final Stream<Object> eventStream = caseAggregate.updateDefendantDetails(
                userId,
                updatedDefendantData.caseId,
                updatedDefendantData.defendantId,
                person,
                clock.now());

        return eventStream.collect(toList());
    }

    private class DefendantData {
        private final UUID caseId = CaseAggregateDefendantTest.this.caseId;
        private final UUID defendantId = CaseAggregateDefendantTest.this.defendantId;
        private final Gender gender = CaseAggregateDefendantTest.this.gender;
        private final String nationalInsuranceNumber = CaseAggregateDefendantTest.this.nationalInsuranceNumber;
        private final String driverNumber = CaseAggregateDefendantTest.this.driverNumber;
        private final String driverLicenceDetails = CaseAggregateDefendantTest.this.driverLicenceDetails;
        private final Language hearingLanguage = CaseAggregateDefendantTest.this.hearingLanguage;
        private final String languageNeeds = CaseAggregateDefendantTest.this.languageNeeds;
        private final ContactDetails contactDetails = CaseAggregateDefendantTest.this.contactDetails;
        private final int numPreviousConvictions = CaseAggregateDefendantTest.this.numPreviousConvictions;
        private final List<Offence> offences = CaseAggregateDefendantTest.this.offences;
        private String title = CaseAggregateDefendantTest.this.title;
        private String firstName = CaseAggregateDefendantTest.this.firstName;
        private String lastName = CaseAggregateDefendantTest.this.lastName;
        private LocalDate dateOfBirth = CaseAggregateDefendantTest.this.dateOfBirth;
        private Address address = CaseAggregateDefendantTest.this.address;
        private final String region = "testregion";
        private final String asn = "asn";
        private final String pncIdentifier = "pncId";

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
