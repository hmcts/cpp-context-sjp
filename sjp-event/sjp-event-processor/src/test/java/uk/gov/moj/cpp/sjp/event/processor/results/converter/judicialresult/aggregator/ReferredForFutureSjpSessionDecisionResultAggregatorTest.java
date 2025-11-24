package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.moj.cpp.sjp.domain.decision.ReferredForFutureSJPSession;

import java.util.Collections;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferredForFutureSjpSessionDecisionResultAggregatorTest extends  BaseDecisionResultAggregatorTest {

    private ReferredForFutureSjpSessionDecisionResultAggregator aggregator;

    @BeforeEach
    public void setUp() {
        super.setUp();
        aggregator = new ReferredForFutureSjpSessionDecisionResultAggregator(jCachedReferenceData);
    }

    @Test
    public void shouldPopulateBasicResult() {
        final ReferredForFutureSJPSession offenceDecision
                = new ReferredForFutureSJPSession(null,
                Collections.singletonList(createOffenceDecisionInformation(offence1Id, PROVED_SJP)));

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(1));
        final JudicialResult judicialResult =  resultsAggregate.getResults(offence1Id).get(0);
        assertThat(judicialResult.getJudicialResultId().toString(), is("0149ab92-5466-11e8-9c2d-fa7ae01bbebc"));
        assertThat(judicialResult.getOrderedDate(), is(resultedOn.format(DATE_FORMAT)));
        assertThat(judicialResult.getResultText(), is("Referred for resulting in another SJP session"));

        assertThat(judicialResult.getJudicialResultPrompts(), is(nullValue()));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)), is(true));

    }
}
