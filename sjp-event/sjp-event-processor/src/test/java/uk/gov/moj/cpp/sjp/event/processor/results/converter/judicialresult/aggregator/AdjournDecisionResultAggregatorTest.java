package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.NO_VERDICT;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;

import java.time.LocalDate;
import java.util.Collections;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AdjournDecisionResultAggregatorTest extends BaseDecisionResultAggregatorTest {

    private static final String ADJOURN_REASON = "Not enough documents present for decision, waiting for document";
    private AdjournDecisionResultAggregator aggregator;

    @Before
    public void setUp() {
        super.setUp();
        aggregator = new AdjournDecisionResultAggregator(jCachedReferenceData);
    }

    @Test
    public void shouldPopulateResultWithRightPrompts() {
        final LocalDate adjournTo = LocalDate.parse("2021-02-07").plusDays(10);
        final Adjourn offenceDecision
                = new Adjourn(null, Collections.singletonList(createOffenceDecisionInformation(offence1Id, NO_VERDICT)), ADJOURN_REASON, adjournTo);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id),
                allOf(hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("f7784e82-20b5-4d2c-b174-6fd57ebf8d7c"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Adjourn to a later SJP hearing session\nAdjourn to date 2021-02-17")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("185e6a04-8b44-430d-8073-d8d12f69733a"))),
                                hasProperty("value", Matchers.is("2021-02-17")))
                        )))))));
    }

}