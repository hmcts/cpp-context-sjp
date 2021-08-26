package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.disability.DisabilityNeeds.NO_DISABILITY_NEEDS;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.PROVED_SJP;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;
import uk.gov.moj.cpp.sjp.domain.decision.ReferForCourtHearing;

import java.util.Collections;
import java.util.UUID;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferForCourtHearingDecisionResultAggregatorTest extends  BaseDecisionResultAggregatorTest {

    private ReferForCourtHearingDecisionResultAggregator aggregator;

    private final UUID REFERRAL_REASON_ID = randomUUID();

    private final DefendantCourtOptions courtOptions =
            new DefendantCourtOptions(
                    new DefendantCourtInterpreter("EN", true),
                    false, NO_DISABILITY_NEEDS);

    @Before
    public void setUp() {
        super.setUp();
        aggregator = new ReferForCourtHearingDecisionResultAggregator(jCachedReferenceData);

        when(referenceDataService.getReferralReasons(any(JsonEnvelope.class)))
                .thenReturn(createObjectBuilder().
                                add("referralReasons",
                                        createArrayBuilder()
                                                .add(createObjectBuilder()
                                                        .add("id", REFERRAL_REASON_ID.toString())
                                                        .add("reason", "referral reason")
                                                        .add("subReason", "referral sub reason")
                                                        .build())).build());
    }

    @Test
    public void shouldPopulateResultWithRightPrompts() {

        final ReferForCourtHearing offenceDecision
                = new ReferForCourtHearing(null,
                Collections.singletonList(createOffenceDecisionInformation(offence1Id, PROVED_SJP)),
                REFERRAL_REASON_ID,
                "Note",
                30,
                courtOptions);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(1));
        final JudicialResult judicialResult =  resultsAggregate.getResults(offence1Id).get(0);
        assertThat(judicialResult.getJudicialResultId().toString(), is("600edfc3-a584-4f9f-a52e-5bb8a99646c1"));
        assertThat(judicialResult.getOrderedDate(), is(resultedOn.format(DATE_FORMAT)));
        assertThat(judicialResult.getResultText(), is("Refer for a full court hearing\n" +
                "Reasons for referring to court referral reason (referral sub reason)"));

        assertThat(resultsAggregate.getFinalOffence(offence1Id),is(nullValue()));
        assertThat(judicialResult.getJudicialResultPrompts().size(), is(1));
        assertThat(judicialResult.getJudicialResultPrompts(),
                hasItem(allOf(
                        Matchers.hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("bca4e07c-17e0-48f1-84f4-7b6ff8bab5e2"))),
                        Matchers.hasProperty("value", Matchers.is("referral reason (referral sub reason)")))
                ));
        assertThat(resultsAggregate.getFinalOffence(offenceDecision.getOffenceIds().get(0)),Matchers.is(nullValue()));
    }
}
