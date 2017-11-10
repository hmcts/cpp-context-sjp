package uk.gov.moj.cpp.sjp.domain.aggregate;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder;

import java.time.ZonedDateTime;

import org.junit.Before;

public abstract class CaseAggregateBaseTest {

    protected Clock clock = new StoppedClock(ZonedDateTime.now());

    protected CaseAggregate sjpCaseAggregate;
    protected Case sjpCase;

    @Before
    public void setUp() {
        sjpCaseAggregate = new CaseAggregate();
        sjpCase = CaseBuilder.aDefaultSjpCase().build();
        sjpCaseAggregate.createCase(sjpCase, clock.now());
    }
}