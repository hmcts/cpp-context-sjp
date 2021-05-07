package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static javax.json.JsonValue.NULL;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.json.schemas.domains.sjp.command.SetOffencesWithdrawalRequestsStatus;
import uk.gov.justice.json.schemas.fragments.sjp.WithdrawalRequestsStatus;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.json.schemas.domains.sjp.event.OffencesWithdrawalRequestsStatusSet;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.state.CaseAggregateState;
import uk.gov.moj.cpp.sjp.event.OffenceWithdrawalRequested;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class OffenceWithdrawalRequestHandlerTest {

    private final UUID withdrawalRequestReasonId = randomUUID();

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAggregate;

    @Mock
    private CaseAggregateState state;

    @Mock
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Mock
    private ProsecutingAuthorityAccess prosecutingAuthorityAccess;

    @Mock
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> argumentCaptor;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(OffencesWithdrawalRequestsStatusSet.class,
            OffenceWithdrawalRequested.class);

    @Spy
    private Clock clock = new StoppedClock(now(UTC));

    @InjectMocks
    private OffenceWithdrawalRequestHandler offenceWithdrawalRequestHandler;

    @Test
    public void shouldHandleOffenceWithdrawalRequestCommands() {
        assertThat(OffenceWithdrawalRequestHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(method("setOffencesWithdrawalRequestsStatus").thatHandles("sjp.command.set-offences-withdrawal-requests-status")));
    }

    @Test
    public void shouldHandleOffenceWithdrawalRequestCorrectly() throws EventStreamException {
        final UUID caseId = randomUUID();
        final UUID offenceId_1 = randomUUID();
        final UUID offenceId_2 = randomUUID();
        final ZonedDateTime setAt = clock.now();
        final String prosecutionAuthority = "ALL";

        final SetOffencesWithdrawalRequestsStatus requestsStatus = new SetOffencesWithdrawalRequestsStatus(caseId, requestPayload(offenceId_1, offenceId_2));
        final UUID userId = randomUUID();
        final Envelope<SetOffencesWithdrawalRequestsStatus> envelope = envelopeFrom(metadataWithRandomUUID("sjp.command.set-offences-withdrawal-requests-status").withUserId(userId.toString()), requestsStatus);

        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(any())).thenReturn(prosecutingAuthorityAccess);
        when(prosecutingAuthorityAccess.getProsecutingAuthority()).thenReturn(prosecutionAuthority);
        when(caseAggregate.requestForOffenceWithdrawal(eq(requestsStatus.getCaseId()), eq(userId), eq(setAt), eq(requestsStatus.getWithdrawalRequestsStatus()), eq(prosecutionAuthority)))
                .thenReturn(Stream.of(new OffencesWithdrawalRequestsStatusSet(caseId, setAt, userId, requestPayload(offenceId_1, offenceId_2)),
                        new OffenceWithdrawalRequested(caseId, offenceId_1, withdrawalRequestReasonId, userId, setAt),
                        new OffenceWithdrawalRequested(caseId, offenceId_2, withdrawalRequestReasonId, userId, setAt)));
        offenceWithdrawalRequestHandler.setOffencesWithdrawalRequestsStatus(envelope);

        verify(eventStream).append(argumentCaptor.capture());

        assertThat(argumentCaptor.getValue(), is(streamContaining(
                jsonEnvelope(
                        withMetadataEnvelopedFrom(JsonEnvelope.envelopeFrom(envelope.metadata(), NULL))
                                .withName("sjp.events.offences-withdrawal-status-set"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.setAt", equalTo(setAt.toString())),
                                withJsonPath("$.setBy", equalTo(userId.toString())),
                                withJsonPath("$.withdrawalRequestsStatus[0].offenceId", equalTo(offenceId_1.toString())),
                                withJsonPath("$.withdrawalRequestsStatus[0].withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId.toString())),
                                withJsonPath("$.withdrawalRequestsStatus[1].offenceId", equalTo(offenceId_2.toString())),
                                withJsonPath("$.withdrawalRequestsStatus[1].withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId.toString()))

                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(JsonEnvelope.envelopeFrom(envelope.metadata(), NULL))
                                .withName("sjp.events.offence-withdrawal-requested"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.offenceId", equalTo(offenceId_1.toString())),
                                withJsonPath("$.withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId.toString())),
                                withJsonPath("$.requestedBy", equalTo(userId.toString())),
                                withJsonPath("$.requestedAt", equalTo(setAt.toString()))

                        ))),
                jsonEnvelope(
                        withMetadataEnvelopedFrom(JsonEnvelope.envelopeFrom(envelope.metadata(), NULL))
                                .withName("sjp.events.offence-withdrawal-requested"),
                        payloadIsJson(allOf(
                                withJsonPath("$.caseId", equalTo(caseId.toString())),
                                withJsonPath("$.offenceId", equalTo(offenceId_2.toString())),
                                withJsonPath("$.withdrawalRequestReasonId", equalTo(withdrawalRequestReasonId.toString())),
                                withJsonPath("$.requestedBy", equalTo(userId.toString())),
                                withJsonPath("$.requestedAt", equalTo(setAt.toString()))

                        )))

        )));
    }

    private List<WithdrawalRequestsStatus> requestPayload(final UUID offenceId_1, final UUID offenceId_2) {
        final List<WithdrawalRequestsStatus> withdrawalRequestsStatuses = new ArrayList<>();
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offenceId_1, withdrawalRequestReasonId));
        withdrawalRequestsStatuses.add(new WithdrawalRequestsStatus(offenceId_2, withdrawalRequestReasonId));
        return withdrawalRequestsStatuses;
    }
}
