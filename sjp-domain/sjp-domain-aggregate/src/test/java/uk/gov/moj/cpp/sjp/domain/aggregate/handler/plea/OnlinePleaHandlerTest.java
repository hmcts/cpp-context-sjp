package uk.gov.moj.cpp.sjp.domain.aggregate.handler.plea;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.codehaus.groovy.runtime.InvokerHelper.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.json.schemas.domains.sjp.events.CaseNoteAdded;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.onlineplea.Offence;
import uk.gov.moj.cpp.sjp.domain.onlineplea.PleadAocpOnline;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.DefendantAcceptedAocp;
import uk.gov.moj.cpp.sjp.event.DefendantAocpPleaRejected;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

public class OnlinePleaHandlerTest {

    private UUID caseId= randomUUID();;
    private UUID defendantId = randomUUID();
    private UUID offenceId = randomUUID();
    private CaseAggregateState caseAggregateState;
    private String caseURN = "TFL18ABC";
    private static final String AOCP_REJECTED_REASON = "Case is already in Ready for Decision stage";
    private static final ZonedDateTime pleadDate = now();;

    @Before
    public void init() {
        caseAggregateState = new CaseAggregateState();
        caseAggregateState.setCaseId(caseId);
        caseAggregateState.setDefendantId(defendantId);
        caseAggregateState.setUrn(caseURN);
    }

    @Test
    public void shouldEmitDefendantAcceptedAocpEvent() {
        final Offence offence = new Offence(offenceId, PleaType.GUILTY, null, null);
        final PleadAocpOnline pleadAocpOnline = new PleadAocpOnline(caseId, defendantId, asList(offence), true, null);
        final Stream<Object> eventStream = OnlinePleaHandler.INSTANCE.pleadAocpAcceptedOnline(pleadAocpOnline, pleadDate, caseAggregateState);

        final List<Object> eventList = eventStream.collect(toList());
        assertThat(eventList, hasItem(allOf(Matchers.instanceOf(DefendantAcceptedAocp.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("defendantId", is(defendantId)),
                Matchers.<CaseNoteAdded>hasProperty("pleadDate", is(pleadDate))
        )));

    }


    @Test
    public void shouldEmitRejected() {
        caseAggregateState.markReady(ZonedDateTime.now(), CaseReadinessReason.PIA);
        final Offence offence = new Offence(offenceId, PleaType.GUILTY, null, null);
        final PleadAocpOnline pleadAocpOnline = new PleadAocpOnline(caseId, defendantId, asList(offence), true, null);
        final Stream<Object> eventStream = OnlinePleaHandler.INSTANCE.pleadAocpAcceptedOnline(

                pleadAocpOnline,
                pleadDate,
                caseAggregateState);

       final List<Object> eventList = eventStream.collect(toList());

       assertThat(eventList, hasItem(allOf(Matchers.instanceOf(DefendantAocpPleaRejected.class),
                Matchers.<CaseNoteAdded>hasProperty("caseId", is(caseId)),
                Matchers.<CaseNoteAdded>hasProperty("defendantId", is(defendantId)),
                Matchers.<CaseNoteAdded>hasProperty("pleadDate", is(pleadDate)),
                Matchers.<CaseNoteAdded>hasProperty("rejectedReason", is(AOCP_REJECTED_REASON))
        )));
    }

}
