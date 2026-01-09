package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.ContactDetails;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.PersonalName;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.legalentity.LegalEntityDefendant;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateRequestAccepted;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantNameUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;

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
public class CaseDefendantHandlerUpdateDefendantDetailsFromCCTest {

    @Mock
    private Address address;

    @Mock
    private Person person;

    @Mock
    private CaseAggregateState state;

    private final CaseDefendantHandler caseDefendantHandler = CaseDefendantHandler.INSTANCE;

    @Test
    public void shouldReturnEmptyStreamWhenCaseNotFound() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(null);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetailsFromCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldReturnDefendantNotFoundEventWhenDefendantDoesNotExist() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(false);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetailsFromCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantNotFound.class));
    }

    @Test
    public void shouldUpdateDefendantDetailsWhenCaseIsCompleted() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);
        // Note: isCaseCompleted() is not checked by updateDefendantDetailsFromCC (key difference from regular update)

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetailsFromCC(
                caseId, defendantId, person, updatedDate, state);

        // then - should succeed even though case is completed (key difference from regular update)
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantDetailsUpdated.class));
    }

    @Test
    public void shouldUpdateDefendantDetailsWhenCaseIsReferredForCourtHearing() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);
        // Note: isCaseReferredForCourtHearing() is not checked by updateDefendantDetailsFromCC (key difference from regular update)

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetailsFromCC(
                caseId, defendantId, person, updatedDate, state);

        // then - should succeed even though case is referred (key difference from regular update)
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantDetailsUpdated.class));
    }

    @Test
    public void shouldUpdateDefendantDetailsSuccessfully() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetailsFromCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantDetailsUpdated.class));
    }

    @Test
    public void shouldRaiseDefendantDetailsUpdateRequestAcceptedWhenDateOfBirthChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final LocalDate newDateOfBirth = LocalDate.of(1990, Month.MAY, 15);
        
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);
        when(state.getDefendantDateOfBirth()).thenReturn(LocalDate.of(1985, Month.JANUARY, 1));
        when(person.getDateOfBirth()).thenReturn(newDateOfBirth);
        when(state.getDefendantFirstName()).thenReturn("John");
        when(state.getDefendantLastName()).thenReturn("Doe");
        when(person.getFirstName()).thenReturn("John");
        when(person.getLastName()).thenReturn("Doe");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetailsFromCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(2))); // At least DefendantDateOfBirthUpdateRequested and DefendantDetailsUpdateRequestAccepted
        
        final boolean hasDobUpdateRequested = eventList.stream()
                .anyMatch(e -> e instanceof DefendantDateOfBirthUpdateRequested);
        assertThat(hasDobUpdateRequested, is(true));
        
        final boolean hasUpdateRequestAccepted = eventList.stream()
                .anyMatch(e -> e instanceof DefendantDetailsUpdateRequestAccepted);
        assertThat(hasUpdateRequestAccepted, is(true));
        
        final DefendantDetailsUpdateRequestAccepted acceptedEvent = (DefendantDetailsUpdateRequestAccepted) eventList.stream()
                .filter(e -> e instanceof DefendantDetailsUpdateRequestAccepted)
                .findFirst()
                .orElse(null);
        assertThat(acceptedEvent, is(notNullValue()));
        assertThat(acceptedEvent.getCaseId(), is(caseId));
        assertThat(acceptedEvent.getDefendantId(), is(defendantId));
        assertThat(acceptedEvent.getNewDateOfBirth(), is(newDateOfBirth));
    }

    @Test
    public void shouldRaiseDefendantDetailsUpdateRequestAcceptedWhenAddressChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final Address oldAddress = new Address("123 Old St", "London", "England", "UK", "Greater London", "SW1A 1AA");
        final Address newAddress = new Address("456 New St", "Manchester", "England", "UK", "Greater Manchester", "M1 1AA");
        
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(oldAddress);
        when(person.getAddress()).thenReturn(newAddress);
        when(state.getDefendantFirstName()).thenReturn("John");
        when(state.getDefendantLastName()).thenReturn("Doe");
        when(person.getFirstName()).thenReturn("John");
        when(person.getLastName()).thenReturn("Doe");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetailsFromCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList =eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(2))); // At least DefendantAddressUpdateRequested and DefendantDetailsUpdateRequestAccepted
        
        final boolean hasAddressUpdateRequested = eventList.stream()
                .anyMatch(e -> e instanceof DefendantAddressUpdateRequested);
        assertThat(hasAddressUpdateRequested, is(true));
        
        final boolean hasUpdateRequestAccepted = eventList.stream()
                .anyMatch(e -> e instanceof DefendantDetailsUpdateRequestAccepted);
        assertThat(hasUpdateRequestAccepted, is(true));
        
        final DefendantDetailsUpdateRequestAccepted acceptedEvent = (DefendantDetailsUpdateRequestAccepted) eventList.stream()
                .filter(e -> e instanceof DefendantDetailsUpdateRequestAccepted)
                .findFirst()
                .orElse(null);
        assertThat(acceptedEvent, is(notNullValue()));
        assertThat(acceptedEvent.getCaseId(), is(caseId));
        assertThat(acceptedEvent.getDefendantId(), is(defendantId));
        assertThat(acceptedEvent.getNewAddress(), is(newAddress));
    }

    @Test
    public void shouldRaiseDefendantDetailsUpdateRequestAcceptedWhenNameChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final PersonalName newPersonalName = new PersonalName("Mr", "John", "Smith");
        
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);
        when(state.getDefendantFirstName()).thenReturn("Jane");
        when(state.getDefendantLastName()).thenReturn("Doe");
        when(person.getFirstName()).thenReturn("John");
        when(person.getLastName()).thenReturn("Smith");
        when(person.getTitle()).thenReturn("Mr");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetailsFromCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(2))); // At least DefendantNameUpdateRequested and DefendantDetailsUpdateRequestAccepted
        
        final boolean hasNameUpdateRequested = eventList.stream()
                .anyMatch(e -> e instanceof DefendantNameUpdateRequested);
        assertThat(hasNameUpdateRequested, is(true));
        
        final boolean hasUpdateRequestAccepted = eventList.stream()
                .anyMatch(e -> e instanceof DefendantDetailsUpdateRequestAccepted);
        assertThat(hasUpdateRequestAccepted, is(true));
        
        final DefendantDetailsUpdateRequestAccepted acceptedEvent = (DefendantDetailsUpdateRequestAccepted) eventList.stream()
                .filter(e -> e instanceof DefendantDetailsUpdateRequestAccepted)
                .findFirst()
                .orElse(null);
        assertThat(acceptedEvent, is(notNullValue()));
        assertThat(acceptedEvent.getCaseId(), is(caseId));
        assertThat(acceptedEvent.getDefendantId(), is(defendantId));
        assertThat(acceptedEvent.getNewPersonalName(), is(newPersonalName));
    }

    @Test
    public void shouldRaiseDefendantDetailsUpdateRequestAcceptedWhenMultipleFieldsChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final LocalDate newDateOfBirth = LocalDate.of(1990, Month.MAY, 15);
        final Address newAddress = new Address("456 New St", "Manchester", "England", "UK", "Greater Manchester", "M1 1AA");
        final PersonalName newPersonalName = new PersonalName("Mr", "John", "Smith");
        final Address oldAddress = new Address("123 Old St", "London", "England", "UK", "Greater London", "SW1A 1AA");
        
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(oldAddress);
        when(person.getAddress()).thenReturn(newAddress);
        when(state.getDefendantDateOfBirth()).thenReturn(LocalDate.of(1985, Month.JANUARY, 1));
        when(person.getDateOfBirth()).thenReturn(newDateOfBirth);
        when(state.getDefendantFirstName()).thenReturn("Jane");
        when(state.getDefendantLastName()).thenReturn("Doe");
        when(person.getFirstName()).thenReturn("John");
        when(person.getLastName()).thenReturn("Smith");
        when(person.getTitle()).thenReturn("Mr");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetailsFromCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(4))); // At least 3 update-requested events + DefendantDetailsUpdateRequestAccepted
        
        final boolean hasUpdateRequestAccepted = eventList.stream()
                .anyMatch(e -> e instanceof DefendantDetailsUpdateRequestAccepted);
        assertThat(hasUpdateRequestAccepted, is(true));
        
        final DefendantDetailsUpdateRequestAccepted acceptedEvent = (DefendantDetailsUpdateRequestAccepted) eventList.stream()
                .filter(e -> e instanceof DefendantDetailsUpdateRequestAccepted)
                .findFirst()
                .orElse(null);
        assertThat(acceptedEvent, is(notNullValue()));
        assertThat(acceptedEvent.getCaseId(), is(caseId));
        assertThat(acceptedEvent.getDefendantId(), is(defendantId));
        assertThat(acceptedEvent.getNewDateOfBirth(), is(newDateOfBirth));
        assertThat(acceptedEvent.getNewAddress(), is(newAddress));
        assertThat(acceptedEvent.getNewPersonalName(), is(newPersonalName));
    }

    @Test
    public void shouldNotRaiseDefendantDetailsUpdateRequestAcceptedWhenNoFieldsChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);
        when(state.getDefendantFirstName()).thenReturn("John");
        when(state.getDefendantLastName()).thenReturn("Doe");
        when(person.getFirstName()).thenReturn("John");
        when(person.getLastName()).thenReturn("Doe");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetailsFromCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1)); // Only DefendantDetailsUpdated, no update-requested events
        
        final boolean hasUpdateRequestAccepted = eventList.stream()
                .anyMatch(e -> e instanceof DefendantDetailsUpdateRequestAccepted);
        assertThat(hasUpdateRequestAccepted, is(false));
    }

    @Test
    public void shouldUpdateLegalEntityDefendantDetailsFromCC() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final Address legalEntityAddress = new Address("456 Business Park", "Corporate City", "", "", "", "EC1A 1BB");
        final ContactDetails legalEntityContact = new ContactDetails("02011111111", "07111111111", "02022222222", "contact@acme.com", "info@acme.com");
        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withName("Acme Corporation Ltd")
                .withAdddres(legalEntityAddress)
                .withContactDetails(legalEntityContact)
                .withIncorporationNumber("INC123456")
                .withPosition("Director")
                .build();

        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(state.getDefendantLegalEntityName()).thenReturn("Old Company Name");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateLegalEntityDefendantDetailsFromCC(
                caseId, defendantId, legalEntityDefendant, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(greaterThanOrEqualTo(2))); // At least DefendantNameUpdateRequested and DefendantDetailsUpdated
        
        final boolean hasNameUpdateRequested = eventList.stream()
                .anyMatch(e -> e instanceof DefendantNameUpdateRequested);
        assertThat(hasNameUpdateRequested, is(true));
        
        final boolean hasDetailsUpdated = eventList.stream()
                .anyMatch(e -> e instanceof DefendantDetailsUpdated);
        assertThat(hasDetailsUpdated, is(true));
        
        final boolean hasUpdateRequestAccepted = eventList.stream()
                .anyMatch(e -> e instanceof DefendantDetailsUpdateRequestAccepted);
        assertThat(hasUpdateRequestAccepted, is(true));
    }

    @Test
    public void shouldUpdateLegalEntityDefendantDetailsFromCCWhenAddressChanged() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final Address oldAddress = new Address("123 Old Street", "Old City", "", "", "", "SW1A 1AA");
        final Address newAddress = new Address("789 New Street", "New City", "", "", "", "SW1B 2CC");
        final ContactDetails legalEntityContact = new ContactDetails(null, null, null, "contact@acme.com", null);
        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withName("Acme Corporation Ltd")
                .withAdddres(newAddress)
                .withContactDetails(legalEntityContact)
                .build();

        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(oldAddress);
        when(state.getDefendantLegalEntityName()).thenReturn("Acme Corporation Ltd");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateLegalEntityDefendantDetailsFromCC(
                caseId, defendantId, legalEntityDefendant, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(2))); // At least DefendantAddressUpdateRequested and DefendantDetailsUpdated
        
        final boolean hasAddressUpdateRequested = eventList.stream()
                .anyMatch(e -> e instanceof DefendantAddressUpdateRequested);
        assertThat(hasAddressUpdateRequested, is(true));
        
        final boolean hasUpdateRequestAccepted = eventList.stream()
                .anyMatch(e -> e instanceof DefendantDetailsUpdateRequestAccepted);
        assertThat(hasUpdateRequestAccepted, is(true));
        
        final DefendantDetailsUpdateRequestAccepted acceptedEvent = (DefendantDetailsUpdateRequestAccepted) eventList.stream()
                .filter(e -> e instanceof DefendantDetailsUpdateRequestAccepted)
                .findFirst()
                .orElse(null);
        assertThat(acceptedEvent, notNullValue());
        assertThat(acceptedEvent.getNewAddress(), notNullValue());
        assertThat(acceptedEvent.getNewAddress().getAddress1(), is("789 New Street"));
    }

    @Test
    public void shouldReturnEmptyStreamWhenCaseNotFoundForLegalEntity() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withName("Test Company")
                .withAdddres(address)
                .withContactDetails(new ContactDetails(null, null, null, "test@company.com", null))
                .build();
        when(state.getCaseId()).thenReturn(null);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateLegalEntityDefendantDetailsFromCC(
                caseId, defendantId, legalEntityDefendant, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldReturnDefendantNotFoundEventWhenDefendantDoesNotExistForLegalEntity() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final LegalEntityDefendant legalEntityDefendant = LegalEntityDefendant.legalEntityDefendant()
                .withName("Test Company")
                .withAdddres(address)
                .withContactDetails(new ContactDetails(null, null, null, "test@company.com", null))
                .build();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(false);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateLegalEntityDefendantDetailsFromCC(
                caseId, defendantId, legalEntityDefendant, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantNotFound.class));
    }
}

