package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest;
import uk.gov.moj.cpp.sjp.domain.aggregate.Session;
import uk.gov.moj.cpp.sjp.domain.aggregate.casestatus.OffenceInformation;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.domain.decision.SessionCourt;
import uk.gov.moj.cpp.sjp.domain.decision.ConvictingInformation;
import uk.gov.moj.cpp.sjp.event.decision.ConvictionCourtResolved;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ResolveConvictingCourtHandlerTest extends CaseAggregateBaseTest {

    private final UUID caseId = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private EventStream caseEventStream;

    @Mock
    private EventStream sessionEventStream;

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private Session session;

    @Mock
    private CaseCommandHandler caseCommandHandler;

    @Mock
    private Stream<Object> caseReceivedTriggeredEvents;

    @Mock
    private CaseAggregateState state;

    @Mock
    Map<UUID, Session> sessionMap;

    @InjectMocks
    private ResolveConvictionCourtHandler resolveConvictionCourtHandler;

    @Spy
    private Clock clock = new StoppedClock(now(UTC));

    public List<OffenceInformation> offenceInformation;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptor;

    @Spy
    private Enveloper enveloper = createEnveloperWithEvents(
            ConvictionCourtResolved.class);

    @Test
    public void shouldHandleSessionCommands() {
        assertThat(ResolveConvictionCourtHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(allOf(
                        method("resolveConvictionCourt").thatHandles("sjp.command.resolve-conviction-court-bdf")
                )));
    }

    @Test
    public void shouldResolveConvictionCourt() throws EventStreamException {
        final JsonEnvelope command = envelopeFrom(metadataWithRandomUUID("sjp.command.resolve-conviction-court-bdf"),
                createObjectBuilder()
                        .add("caseId", caseId.toString())
                        .build());

        final SessionCourt convictingCourt = new SessionCourt("0001", "0002");
        final UUID sessionId = UUID.randomUUID();
        final UUID offenceId = UUID.randomUUID();

        final ConvictingInformation convictingInformation = new ConvictingInformation(ZonedDateTime.now(), convictingCourt, sessionId, offenceId);
        final ConvictionCourtResolved convictionCourtResolved = new ConvictionCourtResolved(caseId, new ArrayList<>(Arrays.asList(convictingInformation)));

        when(eventSource.getStreamById(caseId)).thenReturn(caseEventStream);
        when(aggregateService.get(caseEventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(caseAggregate.resolveConvictionCourt(Mockito.any(), Mockito.any(Map.class))).thenReturn(Stream.of(convictionCourtResolved));

        when(eventSource.getStreamById(sessionId)).thenReturn(sessionEventStream);
        when(aggregateService.get(sessionEventStream, Session.class)).thenReturn(session);

        when(state.getOffenceConvictionInfo(offenceId)).thenReturn(convictingInformation);
        when(state.getOffencesWithConviction()).thenReturn(new HashSet<>(Arrays.asList(offenceId)));
        when(caseAggregate.getState()).thenReturn(state);
        resolveConvictionCourtHandler.resolveConvictionCourt(command);

        verify(caseEventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), is(streamContaining((
                jsonEnvelope(
                        withMetadataEnvelopedFrom(JsonEnvelope.envelopeFrom(command.metadata(), NULL))
                                .withName(ConvictionCourtResolved.EVENT_NAME),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())))
                        ))))));
    }

}
