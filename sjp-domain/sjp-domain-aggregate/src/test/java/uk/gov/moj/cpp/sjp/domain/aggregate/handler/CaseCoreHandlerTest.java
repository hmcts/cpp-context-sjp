package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.ROUND_DOWN;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
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
import uk.gov.moj.cpp.sjp.event.PartialAocpCriteriaNotification;

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
    public void shouldEligibleForAOCPWithCalculatedVictimSurchargeIsLessThenMinimum() {

        setUp(true, true, true, 5, 5);
        final BigDecimal surchargeAmountMin = valueOf(5);
        final BigDecimal surchargeAmountMax = valueOf(2000);
        final BigDecimal surchargeFinePercentage = valueOf(40);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, of(surchargeAmountMin), of(surchargeAmountMax), of(surchargeFinePercentage), empty());

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
        final Object event = eventList.get(0);
        assertThat(event, instanceOf(CaseEligibleForAOCP.class));

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseEligibleForAOCP.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("costs", is(valueOf(5.5))),
                Matchers.<CaseNoteAdded>hasProperty("victimSurcharge", is(surchargeAmountMin.setScale(2, ROUND_DOWN))),
                Matchers.<CaseNoteAdded>hasProperty("aocpTotalCost", is(valueOf(23.00).setScale(2, ROUND_DOWN)))
        )));
    }

    @Test
    public void shouldEligibleForAOCPWithCalculatedVictimSurchargeIsBetweenMinimumAndMaximum() {

        setUp(true, true, true, 400, 400);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, of(ZERO), of(valueOf(2000)), of(valueOf(40)), empty());

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
        final Object event = eventList.get(0);
        assertThat(event, instanceOf(CaseEligibleForAOCP.class));

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseEligibleForAOCP.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("costs", is(valueOf(5.5))),
                Matchers.<CaseNoteAdded>hasProperty("victimSurcharge", is(valueOf(320.0).setScale(2, ROUND_DOWN))),
                Matchers.<CaseNoteAdded>hasProperty("aocpTotalCost", is(valueOf(1128.00).setScale(2, ROUND_DOWN)))
        )));
    }

    @Test
    public void shouldEligibleForAOCPWithCalculatedVictimSurchargeIsMoreThanMaximum(){
        setUp(true, true, true, 400, 400);
        final BigDecimal surchargeAmountMin = ZERO;
        final BigDecimal surchargeAmountMax = valueOf(200);
        final BigDecimal surchargeFinePercentage = valueOf(40);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, of(surchargeAmountMin), of(surchargeAmountMax), of(surchargeFinePercentage), empty());

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
        final Object event = eventList.get(0);
        assertThat(event, instanceOf(CaseEligibleForAOCP.class));

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseEligibleForAOCP.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("costs", is(valueOf(5.5))),
                Matchers.<CaseNoteAdded>hasProperty("victimSurcharge", is(surchargeAmountMax.setScale(2, ROUND_DOWN))),
                Matchers.<CaseNoteAdded>hasProperty("aocpTotalCost", is(valueOf(1008.00).setScale(2, ROUND_DOWN)))
        )));
    }

    @Test
    public void shouldEligibleForAOCPWithCalculatedVictimSurchargeMinAndMaxAreNull(){
        setUp(true, true, true, 400, 400);
        final BigDecimal surchargeFinePercentage = valueOf(50);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, empty(), empty(), of(surchargeFinePercentage), empty());

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
        final Object event = eventList.get(0);
        assertThat(event, instanceOf(CaseEligibleForAOCP.class));

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseEligibleForAOCP.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("costs", is(valueOf(5.5))),
                Matchers.<CaseNoteAdded>hasProperty("victimSurcharge", is(valueOf(400.0).setScale(2, ROUND_DOWN))),
                Matchers.<CaseNoteAdded>hasProperty("aocpTotalCost", is(valueOf(1208.00).setScale(2, ROUND_DOWN)))
        )));
    }

    @Test
    public void shouldEligibleForAOCPWithNonCalculatedVictimSurcharge(){
        setUp(true, true, true, 400, 400);
        final BigDecimal surchargeAmount = valueOf(150);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, empty(), empty(), empty(), of(surchargeAmount));

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
        final Object event = eventList.get(0);
        assertThat(event, instanceOf(CaseEligibleForAOCP.class));

        assertThat(eventList, hasItem(allOf(
                Matchers.instanceOf(CaseEligibleForAOCP.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("costs", is(valueOf(5.5))),
                Matchers.<CaseNoteAdded>hasProperty("victimSurcharge", is(surchargeAmount.setScale(2, ROUND_DOWN))),
                Matchers.<CaseNoteAdded>hasProperty("aocpTotalCost", is(valueOf(958.00).setScale(2, ROUND_DOWN)))
        )));
    }

    @Test
    public void shouldNotEligibleForAOCPIfOffenceAreNotAOCPEligible() {

        setUp(true, false, false, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, of(ZERO), of(valueOf(2000)), of(valueOf(40)), empty());

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
    }


    @Test
    public void shouldNotEligibleForAOCPIfProsecutorIsNotAOCApproved() {

        setUp(true, true, true, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, false, caseAggregateState, of(ZERO), of(valueOf(2000)), of(valueOf(40)), empty());

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldNotEligibleForAOCPIfProsecutorDoesNotOfferAOCP() {

        setUp(false, true, true, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, of(ZERO), of(valueOf(2000)), of(valueOf(40)), empty());

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldNotEligibleForAOCPIfOffenceWithNullAsIsAOCPEligible() {

        setUp(true, null, null, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, of(ZERO), of(valueOf(2000)), of(valueOf(40)), empty());

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
    }

    @Test
    public void shouldNotEligibleForAOCPIfNullAsProsecutorAOCPOffered() {

        setUp(null, true, true, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, of(ZERO), of(valueOf(2000)), of(valueOf(40)), empty());

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(0));
    }

    @Test
    public void shouldNotEligibleForAOCPIfOneOffenceIsNotAOCPEligible() {

        setUp(true, true, false, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, of(ZERO), of(valueOf(2000)), of(valueOf(40)), empty());

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));

        final Object event = eventList.get(0);
        assertThat(event, instanceOf(PartialAocpCriteriaNotification.class));
    }

    @Test
    public void shouldNotEligibleForAOCPIfOneOffenceWithNullAsIsAOCPEligible() {

        setUp(true, true, null, 100, 100);
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, of(ZERO), of(valueOf(2000)), of(valueOf(40)), empty());

        final List<Object> eventList = eventStream.collect(toList());

        assertThat(eventList.size(), is(1));
    }

    @Test
    public void shouldNotEligibleForAOCPWhenAtLeastOneOffenceHasCompensationMoreThanTen(){
        setUp(true, true, true, ONE, valueOf(10.1), valueOf(100), valueOf(100));
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, of(ZERO), of(valueOf(2000)), of(valueOf(40)), empty());

        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(1));
        final Object event = eventList.get(0);
        assertThat(event, instanceOf(PartialAocpCriteriaNotification.class));
    }

    @Test
    public void shouldEligibleForAOCPWhenOffencesHaveCompensationsAreTenOrLessThanTen(){
        setUp(true, true, true, ONE, TEN, valueOf(100), valueOf(100));
        final Stream<Object> eventStream = INSTANCE.resolveCaseAOCPEligibility(caseId, true, caseAggregateState, of(ZERO), of(valueOf(2000)), of(valueOf(40)), empty());

        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList.size(), is(1));
        final Object event = eventList.get(0);
        assertThat(event, instanceOf(CaseEligibleForAOCP.class));
    }


    private void setUp(final Boolean prosecutorOfferAOCP, final Boolean isEligibleAOCPForOffence1,
                       final Boolean isEligibleAOCPForOffence2, long standardPenalty1, long standardPenalty2) {

        setUp(prosecutorOfferAOCP, isEligibleAOCPForOffence1, isEligibleAOCPForOffence2, null, valueOf(2.5), new BigDecimal(standardPenalty1), new BigDecimal(standardPenalty2));
    }


    private void setUp(final Boolean prosecutorOfferAOCP, final Boolean isEligibleAOCPForOffence1,
                       final Boolean isEligibleAOCPForOffence2, BigDecimal compensation1, BigDecimal compensation2, BigDecimal standardPenalty1, BigDecimal standardPenalty2) {

        final AOCPCostOffence offence1 = new AOCPCostOffence(offenceId1, compensation1, standardPenalty1, isEligibleAOCPForOffence1, prosecutorOfferAOCP);
        final AOCPCostOffence offence2 = new AOCPCostOffence(offenceId1, compensation2, standardPenalty2, isEligibleAOCPForOffence2, prosecutorOfferAOCP);

        final AOCPCostDefendant defendant = new AOCPCostDefendant(randomUUID(), asList(offence1, offence2));

        AOCPCost aocpCost = new AOCPCost(caseId, new BigDecimal(5.5), defendant);
        caseAggregateState.addAOCPCost(caseId, aocpCost);
    }
}
