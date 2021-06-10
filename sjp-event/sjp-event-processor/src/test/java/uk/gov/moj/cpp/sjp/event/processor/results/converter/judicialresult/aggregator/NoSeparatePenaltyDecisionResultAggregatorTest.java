package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.moj.cpp.sjp.domain.decision.NoSeparatePenalty;
import uk.gov.moj.cpp.sjp.domain.decision.PressRestriction;

import java.time.ZonedDateTime;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@SuppressWarnings("unchecked")
@RunWith(MockitoJUnitRunner.class)
public class NoSeparatePenaltyDecisionResultAggregatorTest extends  BaseDecisionResultAggregatorTest {

    private NoSeparatePenaltyDecisionResultAggregator aggregator;


    @Before
    public void setUp() {
        super.setUp();
        aggregator = new NoSeparatePenaltyDecisionResultAggregator(jCachedReferenceData);
    }

    @Test
    public void shouldPopulateBasicResult() {

        final NoSeparatePenalty offenceDecision
                = new NoSeparatePenalty(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP),
                false,
                true);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(2));

        assertThat(resultsAggregate.getResults(offence1Id),
                allOf(hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("49939c7c-750f-403e-9ce1-f82e3e568065"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("No separate penalty")),
                        hasProperty("judicialResultPrompts", is(nullValue()))))));
    }

    @Test
    public void shouldPopulateDrivingEndorsementResult() {

        final NoSeparatePenalty offenceDecision
                = new NoSeparatePenalty(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP),
                false,
                true);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(2));

        assertThat(resultsAggregate.getResults(offence1Id),
                allOf(hasItem(allOf(
                                hasProperty("judicialResultId", Matchers.is(fromString("b0aeb4fc-df63-4e2f-af88-97e3f23e847f"))),
                                hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                                hasProperty("resultText", Matchers.is("Driving record endorsed (no points)")),
                                hasProperty("judicialResultPrompts", is(nullValue()))))));
    }

    @Test
    public void shouldPopulatePressRestrictionAppliedResult() {

        final PressRestriction pressRestriction = PressRestriction.requested("Test");

        final NoSeparatePenalty offenceDecision
                = new NoSeparatePenalty(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP),
                false,
                false,
                pressRestriction);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(2));

        assertThat(resultsAggregate.getResults(offence1Id),
                allOf(hasItem(allOf(
                                hasProperty("judicialResultId", Matchers.is(fromString("fcbf777d-1a73-47e7-ab9b-7c51091a022c"))),
                                hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                                hasProperty("resultText", Matchers.is("Direction made under Section 45 of the Youth Justice and Criminal Evidence Act 1999\n" +
                                        "Name of youth Test")),
                                hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                        hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("03983f51-f937-4dd8-9656-276b1ca86785"))),
                                        hasProperty("value", Matchers.is("Test")))
                                )))))));
    }

    @Test
    public void shouldPopulatePressRestrictionRevokedResult() {

        final ZonedDateTime resultedOn = ZonedDateTime.parse("2021-02-07T09:30:00.000Z");

        final PressRestriction pressRestriction = PressRestriction.revoked();

        final NoSeparatePenalty offenceDecision
                = new NoSeparatePenalty(null,
                createOffenceDecisionInformation(offence1Id, PROVED_SJP),
                false,
                false,
                pressRestriction);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(2));

        assertThat(resultsAggregate.getResults(offence1Id),
                allOf(hasItem(allOf(
                        hasProperty("judicialResultId", Matchers.is(fromString("b27b42bf-e20e-46ec-a6e3-5c2e8a076c20"))),
                        hasProperty("orderedDate", Matchers.is(resultedOn.format(DATE_FORMAT))),
                        hasProperty("resultText", Matchers.is("Direction restricting publicity revoked\nDate direction allowing publicity made 2021-02-07")),
                        hasProperty("judicialResultPrompts", allOf(hasItem(allOf(
                                hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("1a7da720-a95a-46e4-b2ee-6b8e9db430cc"))),
                                hasProperty("value", Matchers.is("2021-02-07")))
                        )))))));
    }
}