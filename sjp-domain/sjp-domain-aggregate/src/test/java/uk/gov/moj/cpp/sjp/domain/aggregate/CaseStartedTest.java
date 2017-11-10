package uk.gov.moj.cpp.sjp.domain.aggregate;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.URN;
import static uk.gov.moj.cpp.sjp.domain.testutils.CaseBuilder.aDefaultSjpCase;

import uk.gov.moj.cpp.sjp.domain.util.DefaultTestData;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.event.CaseStarted;

import java.time.ZonedDateTime;

import org.junit.Test;

public class CaseStartedTest extends CaseAggregateBaseTest {

    @Test
    public void testApply_whenCaseStartedEvent() throws Exception {
        CaseStarted caseStarted = new CaseStarted(DefaultTestData.CASE_ID);

        sjpCaseAggregate.apply(caseStarted);

        assertThat("Sets case id", sjpCaseAggregate.getCaseId(), equalTo(DefaultTestData.CASE_ID));
    }

    @Test
    public void testApply_whenCaseCreatedEvent() throws Exception {
        Case aCase = aDefaultSjpCase().build();
        sjpCaseAggregate.createCase(aCase, ZonedDateTime.now());

        assertThat("Sets case id", sjpCaseAggregate.getCaseId(), equalTo(DefaultTestData.CASE_ID));
        assertThat("Sets urn", sjpCaseAggregate.getUrn(), equalTo(URN));
    }




}
