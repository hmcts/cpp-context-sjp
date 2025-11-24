package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.sjp.domain.Priority.LOW;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;

import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.Priority;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.event.CaseMarkedReadyForDecision;

import java.util.UUID;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CaseAggregate#markCaseReadyForDecision}
 */
public class CaseReadyForDecisionMark extends CaseAggregateBaseTest {

    @Test
    public void shouldMarkCaseReadyForDecision() {
        final CaseReadinessReason readinessReason = CaseReadinessReason.PLEADED_GUILTY;
        when(caseAggregate.markCaseReadyForDecision(readinessReason, clock.now()))
                .thenExpect(hasCaseMarkedReadyForDecision(caseId, readinessReason, MAGISTRATE, LOW));

        when(caseAggregate.markCaseReadyForDecision(readinessReason, clock.now()))
                .reason("if we recall the aggregate multiple times it should not add any events!")
                .thenExpect();

        final CaseReadinessReason aDifferentReadinessReason = CaseReadinessReason.PLEADED_GUILTY_REQUEST_HEARING;
        when(caseAggregate.markCaseReadyForDecision(aDifferentReadinessReason, clock.now()))
                .reason("markCaseReadyForDecision with a different reason")
                .thenExpect(hasCaseMarkedReadyForDecision(caseId, aDifferentReadinessReason, DELEGATED_POWERS, LOW));
    }

    private Matcher<Iterable<? super CaseMarkedReadyForDecision>> hasCaseMarkedReadyForDecision(final UUID caseId,
                                                                                                final CaseReadinessReason readinessReason,
                                                                                                final SessionType sessionType,
                                                                                                final Priority priority) {
        return hasItem(allOf(
                Matchers.instanceOf(CaseMarkedReadyForDecision.class),
                Matchers.hasProperty("caseId", is(caseId)),
                Matchers.hasProperty("reason", is(readinessReason)),
                Matchers.hasProperty("markedAt", notNullValue()),
                Matchers.hasProperty("sessionType", is(sessionType)),
                Matchers.hasProperty("priority", is(priority)))
        );
    }

}
