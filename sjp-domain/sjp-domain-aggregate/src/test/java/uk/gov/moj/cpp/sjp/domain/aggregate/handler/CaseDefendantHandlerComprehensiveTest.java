package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.legalentity.LegalEntityDefendant;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdatesAcknowledged;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantPendingChangesRejected;
import uk.gov.moj.cpp.sjp.event.DefendantsNationalInsuranceNumberUpdated;
import uk.gov.moj.cpp.sjp.event.ProsecutionAuthorityAccessDenied;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CaseDefendantHandlerComprehensiveTest {

    @Mock
    private Address address;

    @Mock
    private Address oldAddress;

    @Mock
    private Address newAddress;

    @Mock
    private Person person;

    @Mock
    private CaseAggregateState state;

    private final CaseDefendantHandler caseDefendantHandler = CaseDefendantHandler.INSTANCE;

    // ========== updateDefendantNationalInsuranceNumber Tests ==========

    @Test
    void shouldUpdateDefendantNationalInsuranceNumberSuccessfully() {
        // given
        final UUID userId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final String newNIN = "AB123456C";
        when(state.getCaseId()).thenReturn(UUID.randomUUID());
        when(state.hasDefendant(defendantId)).thenReturn(true);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantNationalInsuranceNumber(
                userId, defendantId, newNIN, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantsNationalInsuranceNumberUpdated.class));
        final DefendantsNationalInsuranceNumberUpdated event = (DefendantsNationalInsuranceNumberUpdated) eventList.get(0);
        assertThat(event.getNationalInsuranceNumber(), is(newNIN));
        assertThat(event.getDefendantId(), is(defendantId));
    }

    @Test
    void shouldReturnCaseNotFoundWhenUpdatingNationalInsuranceNumberCaseNotFound() {
        // given
        final UUID userId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final String newNIN = "AB123456C";
        when(state.getCaseId()).thenReturn(null);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantNationalInsuranceNumber(
                userId, defendantId, newNIN, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(CaseNotFound.class));
    }

    @Test
    void shouldReturnDefendantNotFoundWhenUpdatingNationalInsuranceNumberDefendantNotFound() {
        // given
        final UUID userId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final String newNIN = "AB123456C";
        final UUID caseId = UUID.randomUUID();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(false);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantNationalInsuranceNumber(
                userId, defendantId, newNIN, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantNotFound.class));
    }

    // ========== acknowledgeDefendantDetailsUpdates Tests ==========

    @Test
    void shouldAcknowledgeDefendantDetailsUpdatesSuccessfully() {
        // given
        final UUID defendantId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime acknowledgedAt = ZonedDateTime.now();
        final String userProsecutingAuthority = "TFL";
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getProsecutingAuthority()).thenReturn("TFL");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acknowledgeDefendantDetailsUpdates(
                defendantId, acknowledgedAt, state, userProsecutingAuthority, null);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantDetailsUpdatesAcknowledged.class));
        final DefendantDetailsUpdatesAcknowledged event = (DefendantDetailsUpdatesAcknowledged) eventList.get(0);
        assertThat(event.getCaseId(), is(caseId));
        assertThat(event.getDefendantId(), is(defendantId));
        assertThat(event.getAcknowledgedAt(), is(acknowledgedAt));
    }

    @Test
    void shouldReturnCaseNotFoundWhenAcknowledgingDefendantDetailsUpdatesCaseNotFound() {
        // given
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime acknowledgedAt = ZonedDateTime.now();
        final String userProsecutingAuthority = "TFL";
        when(state.getCaseId()).thenReturn(null);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acknowledgeDefendantDetailsUpdates(
                defendantId, acknowledgedAt, state, userProsecutingAuthority, null);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(CaseNotFound.class));
    }

    @Test
    void shouldReturnDefendantNotFoundWhenAcknowledgingDefendantDetailsUpdatesDefendantNotFound() {
        // given
        final UUID defendantId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime acknowledgedAt = ZonedDateTime.now();
        final String userProsecutingAuthority = "TFL";
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(false);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acknowledgeDefendantDetailsUpdates(
                defendantId, acknowledgedAt, state, userProsecutingAuthority, null);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantNotFound.class));
    }

    @Test
    void shouldReturnProsecutionAuthorityAccessDeniedWhenAcknowledgingWithWrongAuthority() {
        // given
        final UUID defendantId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime acknowledgedAt = ZonedDateTime.now();
        final String userProsecutingAuthority = "TFL";
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getProsecutingAuthority()).thenReturn("POLICE");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acknowledgeDefendantDetailsUpdates(
                defendantId, acknowledgedAt, state, userProsecutingAuthority, null);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(ProsecutionAuthorityAccessDenied.class));
    }

    @Test
    void shouldAcknowledgeDefendantDetailsUpdatesWithAllAuthority() {
        // given
        final UUID defendantId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime acknowledgedAt = ZonedDateTime.now();
        final String userProsecutingAuthority = "ALL";
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getProsecutingAuthority()).thenReturn("POLICE");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acknowledgeDefendantDetailsUpdates(
                defendantId, acknowledgedAt, state, userProsecutingAuthority, null);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantDetailsUpdatesAcknowledged.class));
    }

    @Test
    void shouldAcknowledgeDefendantDetailsUpdatesWithNullDefendantId() {
        // given
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime acknowledgedAt = ZonedDateTime.now();
        final String userProsecutingAuthority = "TFL";
        when(state.getCaseId()).thenReturn(caseId);
        when(state.getProsecutingAuthority()).thenReturn("TFL");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acknowledgeDefendantDetailsUpdates(
                null, acknowledgedAt, state, userProsecutingAuthority, null);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantDetailsUpdatesAcknowledged.class));
    }

    // ========== rejectPendingDefendantChanges Tests ==========

    @Test
    void shouldRejectPendingDefendantChanges() {
        // given
        final UUID defendantId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.rejectPendingDefendantChanges(
                defendantId, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantPendingChangesRejected.class));
        final DefendantPendingChangesRejected event = (DefendantPendingChangesRejected) eventList.get(0);
        assertThat(event.getCaseId(), is(caseId));
        assertThat(event.getDefendantId(), is(defendantId));
        assertThat(event.getRejectedAt(), is(updatedDate));
    }

    // ========== getDefendantWarningEvents Tests ==========

    @Test
    void shouldGetDefendantWarningEventsWhenDateOfBirthChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final LocalDate oldDOB = LocalDate.of(1985, Month.JANUARY, 1);
        final LocalDate newDOB = LocalDate.of(1990, Month.MAY, 15);
        when(state.getCaseId()).thenReturn(caseId);
        when(state.getDefendantDateOfBirth()).thenReturn(oldDOB);
        when(person.getDateOfBirth()).thenReturn(newDOB);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getDefendantWarningEvents(
                person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantDateOfBirthUpdated.class));
        final DefendantDateOfBirthUpdated event = (DefendantDateOfBirthUpdated) eventList.get(0);
        assertThat(event.getOldDateOfBirth(), is(oldDOB));
        assertThat(event.getNewDateOfBirth(), is(newDOB));
    }

    @Test
    void shouldGetDefendantWarningEventsWhenAddressChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final Address oldAddr = new Address("123 Old St", "London", "", "", "", "SW1A 1AA");
        final Address newAddr = new Address("456 New St", "Manchester", "", "", "", "M1 1AA");
        when(state.getCaseId()).thenReturn(caseId);
        when(state.getDefendantAddress()).thenReturn(oldAddr);
        when(person.getAddress()).thenReturn(newAddr);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getDefendantWarningEvents(
                person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantAddressUpdated.class));
        final DefendantAddressUpdated event = (DefendantAddressUpdated) eventList.get(0);
        assertThat(event.getOldAddress(), is(oldAddr));
        assertThat(event.getNewAddress(), is(newAddr));
    }

    @Test
    void shouldGetDefendantWarningEventsWhenNameChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.getDefendantFirstName()).thenReturn("Jane");
        when(state.getDefendantLastName()).thenReturn("Smith");
        when(state.getDefendantTitle()).thenReturn("Ms");
        when(person.getFirstName()).thenReturn("John");
        when(person.getLastName()).thenReturn("Doe");
        when(person.getTitle()).thenReturn("Mr");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getDefendantWarningEvents(
                person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantNameUpdated.class));
        final DefendantNameUpdated event = (DefendantNameUpdated) eventList.get(0);
        assertThat(event.getOldPersonalName().getFirstName(), is("Jane"));
        assertThat(event.getNewPersonalName().getFirstName(), is("John"));
    }

    @Test
    void shouldGetDefendantWarningEventsWhenLegalEntityNameChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.getDefendantLegalEntityName()).thenReturn("Old Company Ltd");
        when(person.getLegalEntityName()).thenReturn("New Company Ltd");
        when(person.getFirstName()).thenReturn(null);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getDefendantWarningEvents(
                person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantNameUpdated.class));
        final DefendantNameUpdated event = (DefendantNameUpdated) eventList.get(0);
        assertThat(event.getOldLegalEntityName(), is("Old Company Ltd"));
        assertThat(event.getNewLegalEntityName(), is("New Company Ltd"));
    }

    @Test
    void shouldGetDefendantWarningEventsWhenMultipleFieldsChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final LocalDate oldDOB = LocalDate.of(1985, Month.JANUARY, 1);
        final LocalDate newDOB = LocalDate.of(1990, Month.MAY, 15);
        final Address oldAddr = new Address("123 Old St", "London", "", "", "", "SW1A 1AA");
        final Address newAddr = new Address("456 New St", "Manchester", "", "", "", "M1 1AA");
        when(state.getCaseId()).thenReturn(caseId);
        when(state.getDefendantDateOfBirth()).thenReturn(oldDOB);
        when(state.getDefendantAddress()).thenReturn(oldAddr);
        when(state.getDefendantFirstName()).thenReturn("Jane");
        when(state.getDefendantLastName()).thenReturn("Smith");
        when(state.getDefendantTitle()).thenReturn("Ms");
        when(person.getDateOfBirth()).thenReturn(newDOB);
        when(person.getAddress()).thenReturn(newAddr);
        when(person.getFirstName()).thenReturn("John");
        when(person.getLastName()).thenReturn("Doe");
        when(person.getTitle()).thenReturn("Mr");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getDefendantWarningEvents(
                person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(3)); // DOB, Address, and Name updated
    }

    @Test
    void shouldNotGetDefendantWarningEventsWhenNoFieldsChanged() {
        // given
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final LocalDate dob = LocalDate.of(1985, Month.JANUARY, 1);
        final Address addr = new Address("123 St", "London", "", "", "", "SW1A 1AA");
        when(state.getDefendantDateOfBirth()).thenReturn(dob);
        when(state.getDefendantAddress()).thenReturn(addr);
        when(state.getDefendantFirstName()).thenReturn("John");
        when(state.getDefendantLastName()).thenReturn("Doe");
        when(person.getDateOfBirth()).thenReturn(dob);
        when(person.getAddress()).thenReturn(addr);
        when(person.getFirstName()).thenReturn("John");
        when(person.getLastName()).thenReturn("Doe");
        when(person.getLegalEntityName()).thenReturn(null);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getDefendantWarningEvents(
                person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(0));
    }

    // ========== getLegalEntityDefendantUpdateRequestedEvents Tests ==========

    @Test
    void shouldGetLegalEntityDefendantUpdateRequestedEventsWhenNameChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final Address legalEntityAddress = new Address("456 Business Park", "Corporate City", "", "", "", "EC1A 1BB");
        final ContactDetails legalEntityContact = new ContactDetails(null, null, null, "contact@acme.com", null);
        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withName("New Company Ltd")
                .withAdddres(legalEntityAddress)
                .withContactDetails(legalEntityContact)
                .build();

        when(state.getCaseId()).thenReturn(caseId);
        when(state.getDefendantLegalEntityName()).thenReturn("Old Company Ltd");
        when(state.getDefendantAddress()).thenReturn(legalEntityAddress);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getLegalEntityDefendantUpdateRequestedEvents(
                legalEntityDefendant, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(1)));
        assertThat(eventList.stream().anyMatch(e -> e instanceof uk.gov.moj.cpp.sjp.event.DefendantNameUpdateRequested), is(true));
    }

    @Test
    void shouldGetLegalEntityDefendantUpdateRequestedEventsWhenNameIsFirstTimeSet() {
        // given
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final Address legalEntityAddress = new Address("456 Business Park", "Corporate City", "", "", "", "EC1A 1BB");
        final ContactDetails legalEntityContact = new ContactDetails(null, null, null, "contact@acme.com", null);
        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withName("New Company Ltd")
                .withAdddres(legalEntityAddress)
                .withContactDetails(legalEntityContact)
                .build();

        when(state.getCaseId()).thenReturn(caseId);
        when(state.getDefendantLegalEntityName()).thenReturn(null); // No existing name
        when(state.getDefendantAddress()).thenReturn(legalEntityAddress);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getLegalEntityDefendantUpdateRequestedEvents(
                legalEntityDefendant, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(1)));
        assertThat(eventList.stream().anyMatch(e -> e instanceof uk.gov.moj.cpp.sjp.event.DefendantNameUpdateRequested), is(true));
    }

    @Test
    void shouldGetLegalEntityDefendantUpdateRequestedEventsWhenAddressChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final Address oldAddrForTest = new Address("123 Old St", "Old City", "", "", "", "SW1A 1AA");
        final Address newAddrForTest = new Address("456 New St", "New City", "", "", "", "SW1B 2CC");
        final ContactDetails legalEntityContact = new ContactDetails(null, null, null, "contact@acme.com", null);
        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withName("Acme Corporation Ltd")
                .withAdddres(newAddrForTest)
                .withContactDetails(legalEntityContact)
                .build();

        when(state.getCaseId()).thenReturn(caseId);
        when(state.getDefendantLegalEntityName()).thenReturn("Acme Corporation Ltd");
        when(state.getDefendantAddress()).thenReturn(oldAddrForTest);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getLegalEntityDefendantUpdateRequestedEvents(
                legalEntityDefendant, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(1)));
        assertThat(eventList.stream().anyMatch(e -> e instanceof uk.gov.moj.cpp.sjp.event.DefendantAddressUpdateRequested), is(true));
    }

    @Test
    void shouldGetLegalEntityDefendantUpdateRequestedEventsWhenBothNameAndAddressChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final Address oldAddrForTest = new Address("123 Old St", "Old City", "", "", "", "SW1A 1AA");
        final Address newAddrForTest = new Address("456 New St", "New City", "", "", "", "SW1B 2CC");
        final ContactDetails legalEntityContact = new ContactDetails(null, null, null, "contact@acme.com", null);
        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withName("New Company Ltd")
                .withAdddres(newAddrForTest)
                .withContactDetails(legalEntityContact)
                .build();

        when(state.getCaseId()).thenReturn(caseId);
        when(state.getDefendantLegalEntityName()).thenReturn("Old Company Ltd");
        when(state.getDefendantAddress()).thenReturn(oldAddrForTest);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getLegalEntityDefendantUpdateRequestedEvents(
                legalEntityDefendant, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(2))); // Name and Address update requested
    }

    // ========== Error Handling Tests ==========

    @Test
    void shouldReturnDefendantDetailsUpdateFailedWhenAddressValidationFails() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(oldAddress);
        when(person.getAddress()).thenReturn(newAddress);
        when(oldAddress.getAddress1()).thenReturn("Old Address Line");
        when(newAddress.getAddress1()).thenReturn(""); // Blank address when old was not blank

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetails(
                UUID.randomUUID(), caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantDetailsUpdateFailed.class));
    }

    @Test
    void shouldReturnDefendantDetailsUpdateFailedWhenLegalEntityAddressValidationFails() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final Address oldAddrForValidation = new Address("123 Old St", "Old City", "", "", "", "SW1A 1AA");
        final Address newAddrForValidation = new Address("", "New City", "", "", "", "SW1B 2CC"); // Blank address1
        final ContactDetails legalEntityContact = new ContactDetails(null, null, null, "contact@acme.com", null);
        final LegalEntityDefendant legalEntityDefendantForValidation = LegalEntityDefendant.legalEntityDefendant()
                .withName("Acme Corporation Ltd")
                .withAdddres(newAddrForValidation)
                .withContactDetails(legalEntityContact)
                .build();

        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(oldAddrForValidation);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateLegalEntityDefendantDetailsFromCC(
                caseId, defendantId, legalEntityDefendantForValidation, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantDetailsUpdateFailed.class));
    }

    // ========== Edge Cases Tests ==========

    @Test
    void shouldHandleNullAddressInValidation() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(null);
        when(person.getAddress()).thenReturn(null);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetails(
                UUID.randomUUID(), caseId, defendantId, person, updatedDate, state);

        // then - should not fail validation
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(1)));
        assertThat(eventList.stream().anyMatch(DefendantDetailsUpdateFailed.class::isInstance), is(false));
    }

    @Test
    void shouldHandleLegalEntityNameChangeWhenStateHasNoName() {
        // given
        final UUID caseId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final Address legalEntityAddress = new Address("456 Business Park", "Corporate City", "", "", "", "EC1A 1BB");
        final ContactDetails legalEntityContact = new ContactDetails(null, null, null, "contact@acme.com", null);
        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withName("New Company Ltd")
                .withAdddres(legalEntityAddress)
                .withContactDetails(legalEntityContact)
                .build();

        when(state.getCaseId()).thenReturn(caseId);
        when(state.getDefendantLegalEntityName()).thenReturn(null);
        when(state.getDefendantAddress()).thenReturn(legalEntityAddress);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getLegalEntityDefendantUpdateRequestedEvents(
                legalEntityDefendant, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(1)));
        assertThat(eventList.stream().anyMatch(e -> e instanceof uk.gov.moj.cpp.sjp.event.DefendantNameUpdateRequested), is(true));
    }

    @Test
    void shouldNotRaiseUpdateRequestedWhenLegalEntityNameUnchanged() {
        // given
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final Address legalEntityAddress = new Address("456 Business Park", "Corporate City", "", "", "", "EC1A 1BB");
        final ContactDetails legalEntityContact = new ContactDetails(null, null, null, "contact@acme.com", null);
        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withName("Acme Corporation Ltd")
                .withAdddres(legalEntityAddress)
                .withContactDetails(legalEntityContact)
                .build();

        when(state.getDefendantLegalEntityName()).thenReturn("Acme Corporation Ltd");
        when(state.getDefendantAddress()).thenReturn(legalEntityAddress);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getLegalEntityDefendantUpdateRequestedEvents(
                legalEntityDefendant, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(0)); // No changes, no events
    }
}

