package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_HANDLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.event.MarkedAsLegalSocChecked;

import java.util.UUID;
import java.util.stream.Stream;

import javax.json.JsonObjectBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MarkAsLegalSocCheckedHandlerTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(MarkedAsLegalSocChecked.class);

    @InjectMocks
    private MarkAsLegalSocCheckedHandler markAsLegalSocCheckedHandler;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Captor
    private ArgumentCaptor<Stream<JsonEnvelope>> captor;

    private UUID checkedBy;
    private UUID caseId;
    private CaseAggregate caseAggregate;

    @Before
    public void setUp() {
        checkedBy = UUID.randomUUID();
        caseId = UUID.randomUUID();

        caseAggregate = new CaseAggregate();
        caseAggregate.getState().markCaseCompleted();
    }

    @Test
    public void shouldHandleMarkAsLegalSocCommands() {
        assertThat(MarkAsLegalSocCheckedHandler.class, isHandlerClass(COMMAND_HANDLER)
                .with(method("markAsLegalSocChecked").thatHandles("sjp.command.mark-as-legal-soc-checked")));
    }

    @Test
    public void shouldMarkAsLegalSocChecked() throws EventStreamException {

        final JsonEnvelope command = createHandlerCommand(checkedBy);

        when(eventSource.getStreamById(any(UUID.class))).thenReturn(eventStream);
        when(aggregateService.get(any(EventStream.class), any())).thenReturn(caseAggregate);
        when(eventSource.getStreamById(caseId)).thenReturn(eventStream);

        markAsLegalSocCheckedHandler.markAsLegalSocChecked(command);

        verify(eventStream).append(captor.capture());

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.marked-as-legal-soc-checked"),
                                payloadIsJson(allOf(
                                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                                        withJsonPath("$.checkedBy", equalTo(checkedBy.toString()))
                                )))
                )));

    }

    private JsonEnvelope createHandlerCommand(final UUID checkedBy) {
        JsonObjectBuilder payload = createObjectBuilder()
                .add("caseId", caseId.toString());

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.mark-as-legal-soc-checked").withUserId(checkedBy.toString()),
                payload.build());
    }
}