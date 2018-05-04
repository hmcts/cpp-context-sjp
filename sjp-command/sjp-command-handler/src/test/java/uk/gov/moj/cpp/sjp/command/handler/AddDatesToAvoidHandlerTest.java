package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataOf;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidReceived;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AddDatesToAvoidHandlerTest extends CaseAggregateBaseTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(DatesToAvoidReceived.class);

    @InjectMocks
    private AddDatesToAvoidHandler addDatesToAvoidHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    private static final String datesToAvoid = "The witness is on holiday for the whole of the month of August 2018";

    @Test
    public void verifyExistenceOfDefendantDetailsUpdatedEvent() throws EventStreamException {
        final JsonEnvelope command = createAddDatesToAvoidHandlerCommand(caseReceivedEvent.getCaseId(), datesToAvoid);

        when(eventSource.getStreamById(caseReceivedEvent.getCaseId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        addDatesToAvoidHandler.addDatesToAvoid(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.dates-to-avoid-received"),
                                payloadIsJson(withJsonPath("$.datesToAvoid", equalTo(datesToAvoid))))
                )));
    }

    private static JsonEnvelope createAddDatesToAvoidHandlerCommand(UUID caseId, String datesToAvoid) {
        JsonObjectBuilder payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("datesToAvoid", datesToAvoid);

        return JsonEnvelopeBuilder.envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.add-dates-to-avoid"),
                payload.build());
    }
}
