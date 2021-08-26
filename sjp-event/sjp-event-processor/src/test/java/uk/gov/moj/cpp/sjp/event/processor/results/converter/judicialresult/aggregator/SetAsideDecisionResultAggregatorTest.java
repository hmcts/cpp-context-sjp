package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.moj.cpp.sjp.domain.decision.SetAside;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SetAsideDecisionResultAggregatorTest extends  BaseDecisionResultAggregatorTest {

    private SetAsideDecisionResultAggregator aggregator;

    @Before
    public void setUp() {
       super.setUp();
        aggregator = new SetAsideDecisionResultAggregator(jCachedReferenceData);
    }

    @Test
    public void shouldPopulateBasicResult() {
        final SetAside offenceDecision
                = new SetAside(null, singletonList(createOffenceDecisionInformation(offence1Id, FOUND_GUILTY)));

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(1));
        final JudicialResult judicialResult =  resultsAggregate.getResults(offence1Id).get(0);
        assertThat(judicialResult.getJudicialResultId().toString(), is("af590f98-21cb-43e7-b992-2a9d444acb2b"));
        assertThat(judicialResult.getOrderedDate(), is(resultedOn.format(DATE_FORMAT)));
        assertThat(judicialResult.getResultText(), is("Set Aside (Single Justice Procedure hearing)"));

        assertThat(judicialResult.getJudicialResultPrompts(), is(nullValue()));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)), is(nullValue()));
    }
}
