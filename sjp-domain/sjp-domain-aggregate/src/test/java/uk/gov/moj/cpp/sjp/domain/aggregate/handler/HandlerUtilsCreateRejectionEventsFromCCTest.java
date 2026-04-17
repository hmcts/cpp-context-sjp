package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class HandlerUtilsCreateRejectionEventsFromCCTest {

    private static final UUID USER_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final UUID UNKNOWN_DEFENDANT_ID = randomUUID();
    private final CaseAggregateState caseAggregateState = new CaseAggregateState();

    @BeforeEach
    void setUp() {
        caseAggregateState.setCaseId(CASE_ID);
        caseAggregateState.addOffenceIdsForDefendant(DEFENDANT_ID, newHashSet());
    }

    @Test
    public void shouldReturnEmptyOptionalWhenCaseNotFound() {
        // given
        final CaseAggregateState stateWithoutCase = new CaseAggregateState();
        stateWithoutCase.setCaseId(null);

        // when
        final Optional<Stream<Object>> result = HandlerUtils.createRejectionEventsForDefendantUpdate(
                "Update defendant detail from CC",
                DEFENDANT_ID,
                stateWithoutCase
        );

        // then
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldReturnDefendantNotFoundWhenDefendantDoesNotExist() {
        // when
        final Optional<Stream<Object>> result = HandlerUtils.createRejectionEventsForDefendantUpdate(
                "Update defendant detail from CC",
                UNKNOWN_DEFENDANT_ID,
                caseAggregateState
        );

        // then
        assertTrue(result.isPresent());
        final List<Object> events = result.get().toList();
        assertThat(events.size(), is(1));
        assertThat(events.get(0), instanceOf(DefendantNotFound.class));
        final DefendantNotFound event = (DefendantNotFound) events.get(0);
        assertThat(event.getDefendantId(), is(UNKNOWN_DEFENDANT_ID));
    }


    @Test
    public void shouldReturnEmptyOptionalWhenCaseIsCompleted() {
        // given
        caseAggregateState.markCaseCompleted();

        // when
        final Optional<Stream<Object>> result = HandlerUtils.createRejectionEventsForDefendantUpdate(
                "Update defendant detail from CC",
                DEFENDANT_ID,
                caseAggregateState
        );

        // then - should NOT reject even if case is completed (this is the key difference from regular update)
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldReturnEmptyOptionalWhenCaseIsReferredForCourtHearing() {
        // given
        caseAggregateState.markCaseReferredForCourtHearing();

        // when
        final Optional<Stream<Object>> result = HandlerUtils.createRejectionEventsForDefendantUpdate(
                "Update defendant detail from CC",
                DEFENDANT_ID,
                caseAggregateState
        );

        // then - should NOT reject even if case is referred (this is the key difference from regular update)
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldReturnEmptyOptionalWhenAllChecksPass() {
        // given - case exists, defendant exists, not assigned to another user, not completed, not referred

        // when
        final Optional<Stream<Object>> result = HandlerUtils.createRejectionEventsForDefendantUpdate(
                "Update defendant detail from CC",
                DEFENDANT_ID,
                caseAggregateState
        );

        // then
        assertFalse(result.isPresent());
    }

    @Test
    public void shouldReturnEmptyOptionalWhenCaseAssignedToSameUser() {
        // given
        caseAggregateState.setAssigneeId(USER_ID);

        // when
        final Optional<Stream<Object>> result = HandlerUtils.createRejectionEventsForDefendantUpdate(
                "Update defendant detail from CC",
                DEFENDANT_ID,
                caseAggregateState
        );

        // then
        assertFalse(result.isPresent());
    }
}

