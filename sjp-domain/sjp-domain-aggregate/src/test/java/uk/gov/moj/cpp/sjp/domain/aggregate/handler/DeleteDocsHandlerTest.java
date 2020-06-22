package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;

import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.domain.decision.FinancialPenalty;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleteDocsRequestRejected;
import uk.gov.moj.cpp.sjp.event.FinancialMeansDeleteDocsStarted;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

public class DeleteDocsHandlerTest {

    private CaseAggregateState caseAggregateState;
    private final UUID caseId = randomUUID();
    private final UUID defendantId = randomUUID();
    private final UUID offenceId1 = randomUUID();
    private final UUID offenceId2 = randomUUID();
    private final UUID offenceId3 = randomUUID();
    private final UUID sessionId = randomUUID();
    private final UUID withdrawalReasonId1 = randomUUID();
    private final UUID withdrawalReasonId2 = randomUUID();

    @Before
    public void setUp() {
        caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(caseId);
        caseAggregateState.setDefendantId(defendantId);
        caseAggregateState.addOffenceIdsForDefendant(defendantId, newHashSet(offenceId1, offenceId2, offenceId3));
    }

    @Test
    public void shouldRaiseDeleteDocsStartedEvent() {

        givenCaseHasWithdrawAndDismissDecisions();

        final Stream<Object> eventStream = DeleteDocsHandler.INSTANCE.deleteDocs(caseAggregateState);
        final FinancialMeansDeleteDocsStarted deleteDocsStarted = collectFirstEvent(eventStream, FinancialMeansDeleteDocsStarted.class);

        assertThat(deleteDocsStarted.getCaseId(), is(caseId));
        assertThat(deleteDocsStarted.getDefendantId(), is(defendantId));
    }

    @Test
    public void shouldNotRaiseDeleteDocsStartedEvent() {
        givenCaseHasWithdrawAndFinancialPenalty();
        final Stream<Object> eventStream = DeleteDocsHandler.INSTANCE.deleteDocs(caseAggregateState);
        assertThat(eventStream.collect(toList()), empty());
    }

    @Test
    public void shouldRejectDeleteDocsStartedEvent() {
        givenCaseHasWithdrawAndDismissDecisions();
        caseAggregateState.setDeleteDocsStarted(true);
        final Stream<Object> eventStream = DeleteDocsHandler.INSTANCE.deleteDocs(caseAggregateState);
        final FinancialMeansDeleteDocsRequestRejected deleteDocsRejected = collectFirstEvent(eventStream, FinancialMeansDeleteDocsRequestRejected.class);

        assertThat(deleteDocsRejected.getCaseId(), is(caseId));
    }

    private void givenCaseHasWithdrawAndDismissDecisions() {
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId2, NO_VERDICT), withdrawalReasonId2),
                new Dismiss(randomUUID(), createOffenceDecisionInformation(offenceId3, FOUND_NOT_GUILTY)));
        caseAggregateState.updateOffenceDecisions(offenceDecisions, sessionId);
        caseAggregateState.markCaseCompleted();
    }

    private void givenCaseHasWithdrawAndFinancialPenalty() {
        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId1, NO_VERDICT), withdrawalReasonId1),
                new Withdraw(randomUUID(), createOffenceDecisionInformation(offenceId2, NO_VERDICT), withdrawalReasonId2),
                new FinancialPenalty(randomUUID(), createOffenceDecisionInformation(offenceId3, FOUND_GUILTY), new BigDecimal(200), new BigDecimal(20),
                        null,false, null, null, false, null,
                        null,null, false,null, null, null, null));
        caseAggregateState.updateOffenceDecisions(offenceDecisions, sessionId);
        caseAggregateState.markCaseCompleted();
    }

    <T> T collectFirstEvent(final Stream<Object> events, final Class<T> eventType) {
        return collectFirstEvent(events.collect(toList()), eventType);
    }

    <T> T collectFirstEvent(final List<Object> events, final Class<T> eventType) {
        final Object firstEvent = events.get(0);
        if (!eventType.isInstance(firstEvent)) {
            fail(format(
                    "Expected a single instance of %s, but found %s.",
                    eventType.getSimpleName(),
                    firstEvent.getClass().getSimpleName()));
        }

        return eventType.cast(firstEvent);
    }
}
