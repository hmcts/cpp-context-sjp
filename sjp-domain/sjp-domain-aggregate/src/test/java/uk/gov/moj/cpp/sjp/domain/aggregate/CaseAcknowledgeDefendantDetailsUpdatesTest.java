package uk.gov.moj.cpp.sjp.domain.aggregate;

import static java.util.UUID.randomUUID;
import static uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest.AggregateTester.when;

import uk.gov.moj.cpp.sjp.event.CaseNotFound;
import uk.gov.moj.cpp.sjp.event.DefendantDetailsUpdatesAcknowledged;
import uk.gov.moj.cpp.sjp.event.DefendantNotFound;

import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CaseAggregate#acknowledgeDefendantDetailsUpdates}
 */
public class CaseAcknowledgeDefendantDetailsUpdatesTest extends CaseAggregateBaseTest {

    public static final String ALL = "ALL";

    @Test
    public void shouldAcknowledgeDefendantDetailsUpdates() {
        when(caseAggregate.acknowledgeDefendantDetailsUpdates(defendantId, clock.now(), ALL, null))
                .thenExpect(new DefendantDetailsUpdatesAcknowledged(caseId, defendantId, clock.now()));
    }

    @Test
    public void shouldNotAcknowledgeDefendantDetailsUpdatesWhenDefendantNotFound() {
        final UUID anotherDefendantId = randomUUID();
        when(caseAggregate.acknowledgeDefendantDetailsUpdates(anotherDefendantId, clock.now(), ALL, null))
                .thenExpect(new DefendantNotFound(anotherDefendantId, "Acknowledge defendant details updates"));
    }

    @Test
    public void shouldNotAcknowledgeDefendantDetailsUpdateWhenNoCaseReceived() {
        resetAggregate(); // cancel case currently received

        defendantId = randomUUID();

        when(caseAggregate.acknowledgeDefendantDetailsUpdates(defendantId, clock.now(), ALL, null))
                .thenExpect(new CaseNotFound(caseId, "Acknowledge defendant details updates"));
    }

}
