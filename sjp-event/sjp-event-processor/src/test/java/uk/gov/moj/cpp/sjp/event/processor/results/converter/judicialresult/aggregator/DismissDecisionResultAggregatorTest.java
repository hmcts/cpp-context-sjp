package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.moj.cpp.sjp.domain.decision.Dismiss;
import uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.DecisionAggregate;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("ALL")
@RunWith(MockitoJUnitRunner.class)
public class DismissDecisionResultAggregatorTest extends BaseDecisionResultAggregatorTest {

    private final DecisionAggregate resultsAggregate = new DecisionAggregate();

    private DismissDecisionResultAggregator aggregator;

    @Before
    public void setUp() {
        super.setUp();
        aggregator = new DismissDecisionResultAggregator(jCachedReferenceData);
    }

    @Test
    public void shouldPopulateResultWithRightPrompts() {
        final Dismiss offenceDecision
                = new Dismiss(null, createOffenceDecisionInformation(offence1Id, FOUND_GUILTY));

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id),
                allOf(hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("14d66587-8fbe-424f-a369-b1144f1684e3"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Dismissed")),
                        hasProperty("judicialResultPrompts", is(nullValue()))))));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)),Matchers.is(true));
    }
}