package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.aggregator;

import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.activemq.artemis.utils.JsonLoader.createArrayBuilder;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;
import static uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult.JCaseResultsConstants.DATE_FORMAT;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.decision.Withdraw;

import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WithDrawDecisionResultAggregatorTest extends  BaseDecisionResultAggregatorTest {

    private WithDrawDecisionResultAggregator aggregator;
    private final UUID withdrawalReason1UUID = randomUUID();
    private final UUID withdrawalReason2UUID = randomUUID();


    @Before
    public void setUp() {
        super.setUp();
        final JsonObject withdrawalReason1 = createObjectBuilder()
                .add("id", withdrawalReason1UUID.toString())
                .add("reasonCodeDescription", "With Drawn reason1")
                .build();
        final JsonObject withdrawalReason2 = createObjectBuilder()
                .add("id", withdrawalReason2UUID.toString())
                .add("reasonCodeDescription", "With Drawn reason2")
                .build();

        when(referenceDataService.getWithdrawalReasons(any(JsonEnvelope.class)))
                .thenReturn(createArrayBuilder()
                        .add(withdrawalReason1)
                        .add(withdrawalReason2).build()
                        .getValuesAs(JsonObject.class));

        aggregator = new WithDrawDecisionResultAggregator(jCachedReferenceData);
    }

    @Test
    public void shouldPopulateResultWithRightPrompts() {
        final Withdraw offenceDecision
                = new Withdraw(null, createOffenceDecisionInformation(offence1Id, FOUND_GUILTY), withdrawalReason1UUID);

        aggregator.aggregate(offenceDecision, sjpSessionEnvelope, resultsAggregate, resultedOn);

        assertThat(resultsAggregate.getResults(offence1Id).size(), is(1));
        final JudicialResult judicialResult =  resultsAggregate.getResults(offence1Id).get(0);
        assertThat(judicialResult.getJudicialResultId().toString(), is("6feb0f2e-8d1e-40c7-af2c-05b28c69e5fc"));
        assertThat(judicialResult.getOrderedDate(), is(resultedOn.format(DATE_FORMAT)));
        assertThat(judicialResult.getResultText(), is("Withdrawn (notice produced)\nReasons With Drawn reason1"));

        assertThat(judicialResult.getJudicialResultPrompts().size(), is(1));
        assertThat(judicialResult.getJudicialResultPrompts(),
                hasItem(allOf(Matchers.hasProperty("judicialResultPromptTypeId", Matchers.is(fromString("318c9eb2-cf3c-4592-a353-1b2166c15f81"))),
                              Matchers.hasProperty("value", Matchers.is("With Drawn reason1")))));
    }
}