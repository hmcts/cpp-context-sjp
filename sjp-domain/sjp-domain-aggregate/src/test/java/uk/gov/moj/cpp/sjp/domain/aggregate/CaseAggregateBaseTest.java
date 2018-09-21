package uk.gov.moj.cpp.sjp.domain.aggregate;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

import java.util.stream.Stream;

import org.junit.Before;

public abstract class CaseAggregateBaseTest {

    protected final Clock clock = new StoppedClock(new UtcClock().now());
    protected CaseAggregate caseAggregate;
    protected Case aCase;
    protected CaseReceived caseReceivedEvent;

    @Before
    public void setUp() {
        caseAggregate = new CaseAggregate();
        aCase = CaseBuilder.aDefaultSjpCase().build();
        caseReceivedEvent = collectSingleEvent(caseAggregate.receiveCase(aCase, clock.now()), CaseReceived.class);
    }

    final CaseReceived buildCaseReceived(Case aCase) {
        return new CaseReceived(
                aCase.getId(),
                aCase.getUrn(),
                aCase.getEnterpriseId(),
                aCase.getProsecutingAuthority(),
                aCase.getCosts(),
                aCase.getPostingDate(),
                aCase.getDefendant(),
                clock.now());
    }

    <T> T collectSingleEvent(Stream<Object> events, Class<T> eventType) {
        return events.findFirst()
                .map(eventType::cast)
                .orElseThrow(() -> new AssertionError("Expected just a single instance of " + eventType.getSimpleName()));
    }

}