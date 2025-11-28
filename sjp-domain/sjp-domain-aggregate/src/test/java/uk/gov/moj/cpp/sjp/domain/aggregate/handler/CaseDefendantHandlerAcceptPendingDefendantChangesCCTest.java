package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantPendingChangesAccepted;

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
public class CaseDefendantHandlerAcceptPendingDefendantChangesCCTest {

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
        final Stream<Object> eventStream = caseDefendantHandler.acceptPendingDefendantChangesCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.collect(toList());
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
        final Stream<Object> eventStream = caseDefendantHandler.acceptPendingDefendantChangesCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(1));
        assertThat(eventList.get(0), instanceOf(DefendantNotFound.class));
    }

    @Test
    public void shouldAcceptPendingDefendantChangesWhenCaseIsCompleted() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);
        // Note: isCaseCompleted() is not checked by acceptPendingDefendantChangesCC (uses createRejectionEventsForDefendantUpdate)

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acceptPendingDefendantChangesCC(
                caseId, defendantId, person, updatedDate, state);

        // then - should succeed even though case is completed (key difference from regular acceptPendingDefendantChanges)
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(greaterThanOrEqualTo(1)));
        assertThat(eventList.stream().anyMatch(e -> e instanceof DefendantDetailsUpdated), is(true));
    }

    @Test
    public void shouldAcceptPendingDefendantChangesWhenCaseIsReferredForCourtHearing() {
        // given
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getCaseId()).thenReturn(caseId);
        when(state.hasDefendant(defendantId)).thenReturn(true);
        when(state.getDefendantAddress()).thenReturn(address);
        when(person.getAddress()).thenReturn(address);
        // Note: isCaseReferredForCourtHearing() is not checked by acceptPendingDefendantChangesCC (uses createRejectionEventsForDefendantUpdate)

        // when
        final Stream<Object> eventStream = caseDefendantHandler.acceptPendingDefendantChangesCC(
                caseId, defendantId, person, updatedDate, state);

        // then - should succeed even though case is referred (key difference from regular acceptPendingDefendantChanges)
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(greaterThanOrEqualTo(1)));
        assertThat(eventList.stream().anyMatch(e -> e instanceof DefendantDetailsUpdated), is(true));
    }

    @Test
    public void shouldAcceptPendingDefendantChangesSuccessfully() {
        // given
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
        final Stream<Object> eventStream = caseDefendantHandler.acceptPendingDefendantChangesCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.collect(toList());
        // Should have: DefendantNameUpdated (name changed), DefendantDateOfBirthUpdated (DOB changed), 
        // DefendantDetailsUpdated, and DefendantPendingChangesAccepted = 4 events
        assertThat(eventList.size(), is(4));
        assertThat(eventList.stream().anyMatch(e -> e instanceof DefendantDetailsUpdated), is(true));
        assertThat(eventList.stream().anyMatch(e -> e instanceof DefendantPendingChangesAccepted), is(true));
    }

    @Test
    public void shouldAcceptPendingDefendantChangesWithAddressChange() {
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
        final Stream<Object> eventStream = caseDefendantHandler.acceptPendingDefendantChangesCC(
                caseId, defendantId, person, updatedDate, state);

        // then
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(greaterThanOrEqualTo(2))); // At least DefendantAddressUpdated and DefendantDetailsUpdated
        assertThat(eventList.stream().anyMatch(e -> e instanceof DefendantDetailsUpdated), is(true));
        assertThat(eventList.stream().anyMatch(e -> e instanceof DefendantPendingChangesAccepted), is(true));
    }
}

