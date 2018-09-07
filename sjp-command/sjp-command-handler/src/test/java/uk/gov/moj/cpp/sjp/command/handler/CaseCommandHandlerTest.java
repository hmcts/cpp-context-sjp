package uk.gov.moj.cpp.sjp.command.handler;

import static java.util.UUID.randomUUID;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.After;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public abstract class CaseCommandHandlerTest {

    protected static final UUID CASE_ID = randomUUID();
    private static final String ACTION_NAME = "actionName";

    @Mock
    protected EventSource eventSource;

    @Mock
    protected Enveloper enveloper;

    @Mock
    protected AggregateService aggregateService;

    @Mock
    protected JsonObjectToObjectConverter converter;

    @Mock
    protected JsonEnvelope jsonEnvelope;

    @Mock
    protected JsonObject jsonObject;

    @Mock
    protected EventStream eventStream;

    @Mock
    protected CaseAggregate caseAggregate;

    @Mock
    protected Function function;

    @Mock
    protected Stream<Object> events;

    @Mock
    protected Stream<JsonEnvelope> jsonEvents;

    @Mock
    protected Metadata metadata;

    @InjectMocks
    private CaseCommandHandler caseCommandHandler;

    private Function<CaseAggregate,Stream<Object>> aggregateFunction = caseAggregate -> events;

    protected UUID userId = randomUUID();

    @Before
    @SuppressWarnings("unchecked")
    public void setupMocks() {
        initMocks(this);
        when(jsonEnvelope.payloadAsJsonObject()).thenReturn(jsonObject);
        when(jsonEnvelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(Optional.of(userId.toString()));
        when(metadata.name()).thenReturn(ACTION_NAME);
        when(jsonObject.getString(CaseCommandHandler.STREAM_ID)).thenReturn(CASE_ID.toString());
        when(eventSource.getStreamById(CASE_ID)).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);
        when(enveloper.withMetadataFrom(jsonEnvelope)).thenReturn(function);
        when(events.map(function)).thenReturn(jsonEvents);
    }

    @After
    @SuppressWarnings("unchecked")
    public void verifyMocks() throws EventStreamException {
        verify(jsonEnvelope, atLeast(1)).payloadAsJsonObject();
        verify(jsonObject, atLeast(1)).getString(CaseCommandHandler.STREAM_ID);
        verify(eventSource).getStreamById(CASE_ID);
        verify(aggregateService).get(eventStream, CaseAggregate.class);

        verify(enveloper).withMetadataFrom(jsonEnvelope);
        verify(eventStream).append(jsonEvents);
        verify(events).map(function);

        verify(jsonEnvelope, atLeast(0)).metadata();
        verify(metadata, atLeast(0)).userId();

        verifyNoMoreInteractions(eventSource);
        verifyNoMoreInteractions(enveloper);
        verifyNoMoreInteractions(aggregateService);
        verifyNoMoreInteractions(converter);
        verifyNoMoreInteractions(jsonEnvelope);
        verifyNoMoreInteractions(jsonObject);
        verifyNoMoreInteractions(eventStream);
        verifyNoMoreInteractions(caseAggregate);
        verifyNoMoreInteractions(function);
        verifyNoMoreInteractions(events);
        verifyNoMoreInteractions(jsonEvents);
    }
}
