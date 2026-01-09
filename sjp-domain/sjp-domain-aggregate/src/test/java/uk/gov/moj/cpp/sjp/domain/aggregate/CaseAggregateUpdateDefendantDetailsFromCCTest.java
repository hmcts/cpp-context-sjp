package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import uk.gov.moj.cpp.sjp.domain.Person;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdated;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CaseAggregateUpdateDefendantDetailsFromCCTest extends CaseAggregateBaseTest {

    private Person person;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        person = aCase.getDefendant();
    }

    @Test
    public void shouldUpdateDefendantDetailsFromCCWhenCaseIsCompleted() {
        // given
        caseAggregate.getState().markCaseCompleted();

        // when
        final List<Object> events = caseAggregate.updateDefendantDetailsFromCC(
                caseId, defendantId, person, ZonedDateTime.now())
                .toList();

        // then - should succeed even though case is completed
        assertThat(events.size(), is(1));
        assertThat(events.get(0), instanceOf(DefendantDetailsUpdated.class));
    }

    @Test
    public void shouldUpdateDefendantDetailsFromCCWhenCaseIsReferredForCourtHearing() {
        // given
        caseAggregate.getState().markCaseReferredForCourtHearing();

        // when
        final List<Object> events = caseAggregate.updateDefendantDetailsFromCC(
                caseId, defendantId, person, ZonedDateTime.now())
                .toList();

        // then - should succeed even though case is referred
        assertThat(events.size(), is(1));
        assertThat(events.get(0), instanceOf(DefendantDetailsUpdated.class));
    }

    @Test
    public void shouldUpdateDefendantDetailsFromCCSuccessfully() {
        // when
        final List<Object> events = caseAggregate.updateDefendantDetailsFromCC(
                caseId, defendantId, person, ZonedDateTime.now())
                .toList();

        // then
        assertThat(events.size(), is(1));
        assertThat(events.get(0), instanceOf(DefendantDetailsUpdated.class));
        final DefendantDetailsUpdated event = (DefendantDetailsUpdated) events.get(0);
        assertThat(event.getCaseId(), is(caseId));
        assertThat(event.getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldReturnEmptyStreamWhenCaseNotFound() {
        // given
        final CaseAggregate newCaseAggregate = new CaseAggregate();
        // state has no caseId set

        // when
        final List<Object> events = newCaseAggregate.updateDefendantDetailsFromCC(
                randomUUID(), randomUUID(), person, ZonedDateTime.now())
                .toList();

        // then - should return empty stream (no event)
        assertThat(events.size(), is(0));
    }
}

