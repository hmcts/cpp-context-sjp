package uk.gov.moj.cpp.sjp.command.handler;

import static java.time.ZoneOffset.UTC;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.sjp.domain.Case;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
public class CaseReceivedHandlerTest {

    private static final UUID CASE_ID = UUID.randomUUID();
    private static final String ACTION_NAME = "actionName";

    @Mock
    private JsonObject commandJsonObject;

    @Mock
    private Case aCase;

    @Mock
    private JsonEnvelope command;

    @Mock
    private JsonObjectToObjectConverter jsonConverter;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventHistory;

    @Mock
    private AggregateService aggregateService;

    @Mock
    private CaseAggregate caseAgg;

    @Mock
    private Enveloper enveloper;

    @Mock
    private Function<Object, JsonEnvelope> enveloperFunction;

    @Mock
    private Stream<JsonEnvelope> finalEventEnvelope;

    @Mock
    private Stream<Object> caseReceivedTriggeredEvents;

    @Mock
    private Metadata metadata;

    @Captor
    private ArgumentCaptor<Case> caseArgCaptor;

    @InjectMocks
    private CaseReceivedHandler caseReceivedHandler;

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @BeforeEach
    public void setup() {
        when(command.payloadAsJsonObject()).thenReturn(commandJsonObject);
        when(commandJsonObject.getString("id")).thenReturn(CASE_ID.toString());
        when(jsonConverter.convert(any(JsonObject.class), any())).thenReturn(aCase);
        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventHistory);
        when(aggregateService.get(any(), eq(CaseAggregate.class))).thenReturn(caseAgg);
        ZonedDateTime now = clock.now(); // this is not inlined as it doesn't work... feel free to try
        when(caseAgg.receiveCase(caseArgCaptor.capture(), eq(now))).thenReturn(caseReceivedTriggeredEvents);
        when(enveloper.withMetadataFrom(any())).thenReturn(enveloperFunction);
        when(caseReceivedTriggeredEvents.map(enveloperFunction)).thenReturn(finalEventEnvelope);
    }

    @Test
    public void testCreateSjpCase() throws Exception {
        caseReceivedHandler.receiveCase(command);

        verify(command, times(2)).payloadAsJsonObject();
        verify(jsonConverter).convert(eq(commandJsonObject), eq(Case.class));
        verify(eventSource).getStreamById(eq(CASE_ID));
        verify(aggregateService).get(eq(eventHistory), eq(CaseAggregate.class));
        verify(caseAgg).receiveCase(aCase, clock.now());
        verify(enveloper).withMetadataFrom(eq(command));
        verify(caseReceivedTriggeredEvents).map(eq(enveloperFunction));
        verify(eventHistory).append(eq(finalEventEnvelope));
        verifyNoMoreInteractionsWithMocks();
    }

    private void verifyNoMoreInteractionsWithMocks() {
        verifyNoMoreInteractions(jsonConverter);
        verifyNoMoreInteractions(eventSource);
        verifyNoMoreInteractions(aggregateService);
        verifyNoMoreInteractions(caseAgg);
        verifyNoMoreInteractions(enveloper);
        verifyNoMoreInteractions(enveloperFunction);
        verifyNoMoreInteractions(caseReceivedTriggeredEvents);
        verifyNoMoreInteractions(eventHistory);
    }
}