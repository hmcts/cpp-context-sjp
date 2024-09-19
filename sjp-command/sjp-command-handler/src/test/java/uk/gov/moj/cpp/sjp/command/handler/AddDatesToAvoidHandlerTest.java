package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
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
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregate;
import uk.gov.moj.cpp.sjp.domain.aggregate.CaseAggregateBaseTest;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidAdded;
import uk.gov.moj.cpp.sjp.event.DatesToAvoidTimerExpired;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AddDatesToAvoidHandlerTest extends CaseAggregateBaseTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(DatesToAvoidAdded.class, DatesToAvoidTimerExpired.class);

    @InjectMocks
    private AddDatesToAvoidHandler addDatesToAvoidHandler;

    @Mock
    private EventSource eventSource;

    @Mock
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Mock
    private EventStream eventStream;

    @Mock
    private AggregateService aggregateService;

    private static final String datesToAvoid = "The witness is on holiday for the whole of the month of August 2018";

    @Test
    public void verifyExistenceOfDefendantDetailsUpdatedEvent() throws EventStreamException {
        final JsonEnvelope command = createAddDatesToAvoidHandlerCommand(caseReceivedEvent.getCaseId(), datesToAvoid);
        when(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(command)).thenReturn(ProsecutingAuthorityAccess.ALL);
        when(eventSource.getStreamById(caseReceivedEvent.getCaseId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        addDatesToAvoidHandler.addDatesToAvoid(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.dates-to-avoid-added"),
                                payloadIsJson(withJsonPath("$.datesToAvoid", equalTo(datesToAvoid))))
                )));
    }

    @Test
    public void verifyDatesToAvoidTimerExpiredRaisedWhenDatesToAvoidTimerElapsed() throws EventStreamException {
        final JsonEnvelope command = createDatesToAvoidElapsedHandlerCommand(caseReceivedEvent.getCaseId());

        when(eventSource.getStreamById(caseReceivedEvent.getCaseId())).thenReturn(eventStream);
        when(aggregateService.get(eventStream, CaseAggregate.class)).thenReturn(caseAggregate);

        addDatesToAvoidHandler.expireDatesToAvoidTimer(command);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                withMetadataEnvelopedFrom(command)
                                        .withName("sjp.events.dates-to-avoid-expired"),
                                payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString()))))
                )));
    }

    private static JsonEnvelope createAddDatesToAvoidHandlerCommand(final UUID caseId, final String datesToAvoid) {
        JsonObjectBuilder payload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("datesToAvoid", datesToAvoid);

        return envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.add-dates-to-avoid"),
                payload.build());
    }

    private static JsonEnvelope createDatesToAvoidElapsedHandlerCommand(final UUID caseId) {
        JsonObjectBuilder payload = createObjectBuilder()
                .add("caseId", caseId.toString());

        return JsonEnvelope.envelopeFrom(
                metadataOf(randomUUID(), "sjp.command.expire-dates-to-avoid-timer"),
                payload.build());
    }
}
