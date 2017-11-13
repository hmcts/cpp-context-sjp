
package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.aggregate.AggregateService;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.aggregate.DefendantAggregate;
import uk.gov.moj.cpp.sjp.event.PersonInfoAdded;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AddPersonInfoHandlerTest extends BasePersonInfoHandlerTest {

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @Mock
    private AggregateService aggregateService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(PersonInfoAdded.class);

    @InjectMocks
    private AddPersonInfoHandler addPersonInfoHandler;

    final UUID id = UUID.randomUUID();

    @Test
    public void shouldAddPersonInfo() throws EventStreamException {
        DefendantAggregate defendantAggregate = new DefendantAggregate();
        when(eventSource.getStreamById(id)).thenReturn(eventStream);
        when(eventSource.getStreamById(personId)).thenReturn(eventStream);

        when(aggregateService.get(eventStream, DefendantAggregate.class)).thenReturn(defendantAggregate);

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("structure.command.add-person-info"))
                .withPayloadOf(id.toString(), "id")
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(personId.toString(), "personId")
                .withPayloadOf(firstName, "firstName")
                .withPayloadOf(lastName, "lastName")
                .withPayloadOf(dateOfBirth, "dateOfBirth")
                .build();

        addPersonInfoHandler.addPersonInfo(command);

        assertThat(eventStream, eventStreamAppendedWith(streamContaining(jsonEnvelope(
                withMetadataEnvelopedFrom(command)
                        .withName("sjp.events.person-info-added"),
                payloadIsJson(allOf(
                        withJsonPath("$.id", is(id.toString())),
                        withJsonPath("$.caseId", is(caseId.toString())),
                        withJsonPath("$.personId", is(personId.toString())),
                        withJsonPath("$.firstName", is(firstName)),
                        withJsonPath("$.lastName", is(lastName))
                ))))));
    }
}
