package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.math.BigDecimal.ROUND_DOWN;
import static java.util.Arrays.asList;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static uk.gov.moj.cpp.sjp.domain.aggregate.handler.CaseCoreHandler.INSTANCE;
import static java.math.BigDecimal.valueOf;

import uk.gov.justice.json.schemas.domains.sjp.events.CaseNoteAdded;
import uk.gov.moj.cpp.sjp.domain.AOCPCost;
import uk.gov.moj.cpp.sjp.domain.AOCPCostDefendant;
import uk.gov.moj.cpp.sjp.domain.AOCPCostOffence;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.CaseEligibleForAOCP;

import java.math.BigDecimal;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CaseCoreHandlerTest {

    private CaseAggregateState caseAggregateState = new CaseAggregateState();
    private final UUID caseId = randomUUID();
    private final UUID offenceId1 = randomUUID();


    @Test
    public void shouldEligibleForAOCPWithMinimumVictimCharge() {

        setUp(true, true, true, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
        final Object event = eventList.get(0);
        assertThat(event, instanceOf(CaseEligibleForAOCP.class));

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseEligibleForAOCP.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("costs", is(valueOf(5.5))),
                Matchers.<CaseNoteAdded>hasProperty("victimSurcharge", is(valueOf(34.00).setScale(2, ROUND_DOWN))),
                Matchers.<CaseNoteAdded>hasProperty("aocpTotalCost", is(valueOf(244.50).setScale(2, ROUND_DOWN)))
        )));
    }

    @Test
    public void shouldEligibleForAOCPWithVictimChargeWhichIsMoreThanMinimumCharge() {

        setUp(true, true, true, 400, 400);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
        final Object event = eventList.get(0);
        assertThat(event, instanceOf(CaseEligibleForAOCP.class));

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseEligibleForAOCP.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("costs", is(valueOf(5.5))),
                Matchers.<CaseNoteAdded>hasProperty("victimSurcharge", is(valueOf(80.0).setScale(2, ROUND_DOWN))),
                Matchers.<CaseNoteAdded>hasProperty("aocpTotalCost", is(valueOf(890.5).setScale(2, ROUND_DOWN)))
        )));
    }

    @Test
    public void shouldNotEligibleForAOCPIfOffenceAreNotAOCPEligible() {

        setUp(true, false, false, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
    }


    @Test
    public void shouldNotEligibleForAOCPIfProsecutorIsNotAOCApproved() {

        setUp(true, true, true, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, false, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldNotEligibleForAOCPIfProsecutorDoesNotOfferAOCP() {

        setUp(false, true, true, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldNotEligibleForAOCPIfOffenceWithNullAsIsAOCPEligible() {

        setUp(true, null, null, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
    }

    @Test
    public void shouldNotEligibleForAOCPIfNullAsProsecutorAOCPOffered() {

        setUp(null, true, true, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldNotEligibleForAOCPIfOneOffenceIsNotAOCPEligible() {

        setUp(true, true, false, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
    }

    @Test
    public void shouldNotEligibleForAOCPIfOneOffenceWithNullAsIsAOCPEligible() {

        setUp(true, true, null, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
    }

    private void setUp(final Boolean prosecutorOfferAOCP, final Boolean isEligibleAOCPForOffence1,
                       final Boolean isEligibleAOCPForOffence2, long standardPenalty1, long standardPenalty2) {

        final AOCPCostOffence offence1 = new AOCPCostOffence(offenceId1, BigDecimal.valueOf(2.5), new BigDecimal(standardPenalty1), isEligibleAOCPForOffence1, prosecutorOfferAOCP);
        final AOCPCostOffence offence2 = new AOCPCostOffence(offenceId1, BigDecimal.valueOf(2.5), new BigDecimal(standardPenalty2), isEligibleAOCPForOffence2, prosecutorOfferAOCP);

        final AOCPCostDefendant defendant = new AOCPCostDefendant(randomUUID(), asList(offence1, offence2));

        AOCPCost aocpCost = new AOCPCost(caseId, new BigDecimal(5.5), defendant);
        caseAggregateState.addAOCPCost(caseId, aocpCost);
    }
}
