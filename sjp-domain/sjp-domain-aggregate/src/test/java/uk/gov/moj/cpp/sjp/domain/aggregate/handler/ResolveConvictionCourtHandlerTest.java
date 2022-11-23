package uk.gov.moj.cpp.sjp.domain.aggregate.handler;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static java.util.Objects.nonNull;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.decision.OffenceDecisionInformation.createOffenceDecisionInformation;
import static uk.gov.moj.cpp.sjp.domain.verdict.VerdictType.FOUND_GUILTY;

import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.Adjourn;
import uk.gov.moj.cpp.sjp.domain.decision.OffenceDecision;
import uk.gov.moj.cpp.sjp.event.CaseReceived;
import uk.gov.moj.cpp.sjp.event.decision.ConvictionCourtResolved;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.collections.map.HashedMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ResolveConvictionCourtHandlerTest {

    private CaseAggregateState caseAggregateState;
    private Map<UUID, Session> sessionMap;
    private Map<UUID, CaseReceived> caseMap;
    private final String adjournmentReason = "Not enough documents for decision";
    private final UUID decisionId = randomUUID();
    private final UUID sessionId = randomUUID();
    private final UUID caseId = randomUUID();
    private final UUID offenceId = randomUUID();

    @Before
    public void onceBeforeEachTest() {
        caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(caseId);
        sessionMap = new HashedMap();
        caseMap = new HashedMap();
    }

    @Test
    public void shouldResolveConvictionCourt() {
        final LocalDate adjournedTo = LocalDate.now().plusDays(10);
        final ZonedDateTime savedAt = ZonedDateTime.now();
        final Session session = new Session();
        session.setCourtHouseCode("1234");
        session.setLocalJusticeAreaNationalCourtCode("0001");
        sessionMap.put(offenceId, session);

        final List<OffenceDecision> offenceDecisions = newArrayList(
                new Adjourn(randomUUID(), asList(
                        createOffenceDecisionInformation(offenceId, FOUND_GUILTY)),
                        adjournmentReason, adjournedTo));

        caseAggregateState.updateOffenceConvictionDetails(savedAt, offenceDecisions, sessionId);

        final Stream<Object> eventStream =
                ResolveConvictionCourtHandler.INSTANCE.resolveConvictionCourt(caseId, caseAggregateState, sessionMap);

        assertTrue("Event stream is created", nonNull(eventStream));
        final List<Object> eventList = eventStream.collect(toList());
        thenConvictionCourtResolved(eventList);
    }


    private void thenConvictionCourtResolved(final List<Object> eventList) {
        assertThat(eventList, hasItem(allOf(
                instanceOf(ConvictionCourtResolved.class),
                hasProperty("caseId", equalTo(caseAggregateState.getCaseId())),
                hasProperty("convictingInformations")
        )));

        final ConvictionCourtResolved convictionCourtResolved = (ConvictionCourtResolved) eventList.get(0);
        assertThat(convictionCourtResolved.getConvictingInformations(), hasItem(allOf(
                hasProperty("offenceId", equalTo(offenceId)),
                hasProperty("sessionId", equalTo(sessionId))
        )));
    }

}
