package uk.gov.moj.cpp.sjp.domain.aggregate;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;
import uk.gov.moj.cpp.sjp.event.CaseReceived;

import org.junit.Before;

public abstract class CaseAggregateBaseTest {

    protected final Clock clock = new StoppedClock(new UtcClock().now());
    protected CaseAggregate caseAggregate;
    Case aCase;
    protected CaseReceived caseReceivedEvent;

    @Before
    public void setUp() {
        caseAggregate = new CaseAggregate();
        aCase = CaseBuilder.aDefaultSjpCase().build();
        caseReceivedEvent = caseAggregate.receiveCase(aCase, clock.now())
                .findFirst()
                .map(CaseReceived.class::cast)
                .orElseThrow(() -> new AssertionError("Expected just a single instance of " + CaseReceived.class.getSimpleName()));
    }

    CaseReceived buildCaseReceived(Case aCase) {
        return new CaseReceived(
                aCase.getId(),
                aCase.getUrn(),
                aCase.getProsecutingAuthority(),
                aCase.getCosts(),
                aCase.getPostingDate(),
                aCase.getDefendant(),
                clock.now());
    }
}