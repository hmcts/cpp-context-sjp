package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.DefendantDateOfBirthUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDetailUpdateRequested;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdateFailed;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;

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
        final List<Object> eventList = eventStream.collect(toList());
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
        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
        final Object o = eventList.get(0);
        assertThat(o, instanceOf(DefendantDetailsUpdated.class));
    }

    @Test
    public void shouldRaiseDOBUpdateRequestEventWhenDOBIsNotProvidedAtCaseCreationAndProvidedInOnlinePlea(){
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(person.getDateOfBirth()).thenReturn(LocalDate.of(1999, Month.APRIL, 1));
        final Stream<Object> eventStream = caseDefendantHandler.getDefendantUpdateRequestedEvents(person, updatedDate, true, state);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(2));
        final Object o = eventList.get(0);
        assertThat(o, instanceOf(DefendantDateOfBirthUpdateRequested.class));
        final Object o1 = eventList.get(1);
        assertThat(o1, instanceOf(DefendantDetailUpdateRequested.class));
    }

    @Test
    public void shouldRaiseDOBUpdateRequestEventWhenDOBIsProvidedAtCaseCreationAndNotProvidedInOnlinePlea(){
        final ZonedDateTime updatedDate = ZonedDateTime.now();
        when(state.getDefendantDateOfBirth()).thenReturn(LocalDate.of(1999, Month.APRIL, 1));
        final Stream<Object> eventStream = caseDefendantHandler.getDefendantUpdateRequestedEvents(person, updatedDate, true, state);
        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(2));
        final Object o = eventList.get(0);
        assertThat(o, instanceOf(DefendantDateOfBirthUpdateRequested.class));
        final Object o1 = eventList.get(1);
        assertThat(o1, instanceOf(DefendantDetailUpdateRequested.class));
    }
}
