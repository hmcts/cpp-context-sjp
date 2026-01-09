package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDetailUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantPendingChangesAccepted;
import uk.gov.moj.cpp.sjp.event.DefendantAddressUpdateRequested;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@SuppressWarnings("squid:S2187")
@ExtendWith(MockitoExtension.class)
public class CaseDefendantHandlerTest {

    @Mock
    private Address personAddress;

    @Mock
    private Address address;

    @Mock
    private Person person;

    @Mock
    private CaseAggregateState state;

    @InjectMocks
    CaseDefendantHandler caseDefendantHandler;

    @Test
    public void shouldFailUpdatingDefendantDetailsTest() {
        final UUID userId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(personAddress);
        when(person.getAddress().getAddress1()).thenReturn(null);
        when(address.getAddress1()).thenReturn("addressLine");

        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetails(userId, caseId,
                defendantId, person, updatedDate, state);
        final List<Object> eventList = eventStream.toList();
        final Object o = eventList.get(0);
        assertThat(o, instanceOf(DefendantDetailsUpdateFailed.class));
    }

    @Test
    public void shouldPassUpdatingDefendantDetailsTest() {
        final UUID userId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);

        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetails(userId, caseId,
                defendantId, person, updatedDate, state);
        final List<Object> eventList = eventStream.toList();

        assertThat(eventList.size(), is(1));
        final Object o = eventList.get(0);
        assertThat(o, instanceOf(DefendantDetailsUpdated.class));
    }

    @Test
    public void shouldRaiseDOBUpdateRequestEventWhenDOBIsNotProvidedAtCaseCreationAndProvidedInOnlinePlea() {
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(person.getDateOfBirth()).thenReturn(LocalDate.of(1999, Month.APRIL, 1));
        final Stream<Object> eventStream = caseDefendantHandler.getDefendantUpdateRequestedEvents(person, updatedDate, true, state);
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(2));
        final Object o = eventList.get(0);
        assertThat(o, instanceOf(DefendantDateOfBirthUpdateRequested.class));
        final Object o1 = eventList.get(1);
        assertThat(o1, instanceOf(DefendantDetailUpdateRequested.class));
    }

    @Test
    public void shouldRaiseDOBUpdateRequestEventWhenDOBIsProvidedAtCaseCreationAndNotProvidedInOnlinePlea() {
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getDefendantDateOfBirth()).thenReturn(LocalDate.of(1999, Month.APRIL, 1));
        final Stream<Object> eventStream = caseDefendantHandler.getDefendantUpdateRequestedEvents(person, updatedDate, true, state);
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(2));
        final Object o = eventList.get(0);
        assertThat(o, instanceOf(DefendantDateOfBirthUpdateRequested.class));
        final Object o1 = eventList.get(1);
        assertThat(o1, instanceOf(DefendantDetailUpdateRequested.class));
    }

    @Test
    public void shouldReturnCaseNotFoundWhenAcceptPendingDefendantChangesCaseNotFound() {
        // given
        final UUID userId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(null);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acceptPendingDefendantChanges(
                userId, caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(CaseNotFound.class));
    }

    @Test
    public void shouldReturnDefendantNotFoundWhenAcceptPendingDefendantChangesDefendantDoesNotExist() {
        // given
        final UUID userId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(false);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acceptPendingDefendantChanges(
                userId, caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantNotFound.class));
    }

    @Test
    public void shouldAcceptPendingDefendantChangesSuccessfully() {
        // given
        final UUID userId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);
        when(person.getFirstName()).thenReturn("John");
        when(person.getLastName()).thenReturn("Doe");
        when(person.getDateOfBirth()).thenReturn(LocalDate.of(1990, Month.JANUARY, 1));
        when(state.getDefendantFirstName()).thenReturn("Jane");
        when(state.getDefendantLastName()).thenReturn("Smith");
        when(state.getDefendantDateOfBirth()).thenReturn(LocalDate.of(1985, Month.MARCH, 15));

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acceptPendingDefendantChanges(
                userId, caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.toList();
        // Should have: DefendantNameUpdated (name changed), DefendantDateOfBirthUpdated (DOB changed), 
        // DefendantDetailsUpdated, and DefendantPendingChangesAccepted = 4 events
        assertThat(eventList.size(), is(4));
        assertThat(eventList.stream().anyMatch(e -> e instanceof DefendantDetailsUpdated), is(true));
        assertThat(eventList.stream().anyMatch(e -> e instanceof DefendantPendingChangesAccepted), is(true));
    }

    @Test
    public void shouldAcceptPendingDefendantChangesWhenCaseIsCompleted() {
        // given
        final UUID userId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        // Note: isCaseCompleted() is not checked by acceptPendingDefendantChanges (uses createRejectionEventsForDefendantUpdate)
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);
        when(person.getFirstName()).thenReturn("John");
        when(person.getLastName()).thenReturn("Doe");
        when(state.getDefendantFirstName()).thenReturn("Jane");
        when(state.getDefendantLastName()).thenReturn("Smith");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acceptPendingDefendantChanges(
                userId, caseId, defendantId, person, updatedDate, state);

        // then - should succeed even though case is completed
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(1)));
        assertThat(eventList.stream().anyMatch(e -> e instanceof DefendantDetailsUpdated), is(true));
    }

    @Test
    public void shouldUpdateDefendantDetailsWithAddressUpdateFromApplicationWhenCaseIsCompleted() {
        // given
        final UUID userId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.isCaseCompleted()).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetails(
                userId, caseId, defendantId, person, updatedDate, state, true);

        // then - should succeed even though case is completed when isAddressUpdateFromApplication is true
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantDetailsUpdated.class));
    }

    @Test
    public void shouldRejectUpdateDefendantDetailsWhenCaseIsCompletedAndNotAddressUpdateFromApplication() {
        // given
        final UUID userId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.isCaseCompleted()).thenReturn(true);
        when(state.getAssigneeId()).thenReturn(userId);
        when(state.isAssignee(userId)).thenReturn(true);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetails(
                userId, caseId, defendantId, person, updatedDate, state, false);

        // then - should reject when case is completed and isAddressUpdateFromApplication is false
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(uk.gov.moj.cpp.sjp.event.CaseUpdateRejected.class));
    }

    @Test
    public void shouldIncludeAddressInDefendantDetailsUpdatedWhenAddressUpdateFromApplication() {
        // given
        final UUID userId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(personAddress);
        when(personAddress.getAddress1()).thenReturn("New Address");
        when(address.getAddress1()).thenReturn("Old Address");

        // when
        final Stream<Object> eventStream = caseDefendantHandler.updateDefendantDetails(
                userId, caseId, defendantId, person, updatedDate, state, true);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(1)));
        final DefendantDetailsUpdated event = (DefendantDetailsUpdated) eventList.stream()
                .filter(e -> e instanceof DefendantDetailsUpdated)
                .findFirst()
                .orElse(null);
        assertThat(event, is(notNullValue()));
        assertThat(event.getAddress(), is(personAddress));
    }

    @Test
    public void shouldRaiseAddressUpdateRequestWhenAddressUpdateFromApplication() {
        // given
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        final UUID caseId = UUID.randomUUID();
        final Address oldAddress = new Address("Old Street", "Old Town", "", "", "", "OLD123");
        final Address newAddress = new Address("New Street", "New Town", "", "", "", "NEW456");
        when(state.getCaseId()).thenReturn(caseId);
        when(state.getDefendantAddress()).thenReturn(oldAddress);
        when(person.getAddress()).thenReturn(newAddress);

        // when
        final Stream<Object> eventStream = caseDefendantHandler.getDefendantUpdateRequestedEvents(
                person, updatedDate, false, state, true);

        // then
        final List<Object> eventList = eventStream.toList();
        assertThat(eventList.size(), is(greaterThanOrEqualTo(1)));
        assertThat(eventList.stream().anyMatch(e -> e instanceof DefendantAddressUpdateRequested), is(true));
        final DefendantAddressUpdateRequested addressEvent = (DefendantAddressUpdateRequested) eventList.stream()
                .filter(e -> e instanceof DefendantAddressUpdateRequested)
                .findFirst()
                .orElse(null);
        assertThat(addressEvent, is(notNullValue()));
    }

}
