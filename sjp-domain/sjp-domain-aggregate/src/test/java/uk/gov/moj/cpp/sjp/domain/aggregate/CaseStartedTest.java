package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.aDefaultSjpCase;

import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;
import uk.gov.moj.cpp.sjp.event.CaseStarted;

import org.junit.Test;

public class CaseStartedTest extends CaseAggregateBaseTest {

    @Test
    public void testApply_whenCaseStartedEvent() {
        CaseStarted caseStarted = new CaseStarted(DefaultTestData.CASE_ID);

        caseAggregate.apply(caseStarted);

        assertThat("Sets case id", caseAggregate.getCaseId(), equalTo(DefaultTestData.CASE_ID));
    }

    @Test
    public void testApply_whenCaseCreatedEvent() {
        Case aCase = aDefaultSjpCase().build();
        caseAggregate.receiveCase(aCase, clock.now());

        assertThat("Sets case id", caseAggregate.getCaseId(), equalTo(aCase.getId()));
        assertThat("Sets urn", caseAggregate.getUrn(), equalTo(aCase.getUrn()));
    }


}
