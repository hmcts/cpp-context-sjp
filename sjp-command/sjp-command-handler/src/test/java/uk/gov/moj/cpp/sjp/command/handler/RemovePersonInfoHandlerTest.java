package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.fromString;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.PersonInfoRemoved;

import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RemovePersonInfoHandlerTest {

    @Mock
    private JsonObject payload;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloperWithEvents(PersonInfoRemoved.class);

    @InjectMocks
    private RemovePersonInfoHandler removePersonInfoHandler;

    @Test
    public void shouldPerformRemovePersonInfo() throws EventStreamException {

        final String personInfoId = UUID.randomUUID().toString();
        final String caseId = UUID.randomUUID().toString();
        final String personId = UUID.randomUUID().toString();
        when(eventSource.getStreamById(fromString(personId))).thenReturn(eventStream);

        final JsonEnvelope command = envelope()
                .with(metadataWithRandomUUID("sjp.command.person-info-removed"))
                .withPayloadOf(personInfoId, "personInfoId")
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(personId, "personId")
                .build();
        removePersonInfoHandler.removePersonInfo(command);

        assertThat(eventStream, eventStreamAppendedWith(streamContaining(jsonEnvelope(
                withMetadataEnvelopedFrom(command)
                        .withName("sjp.events.person-info-removed"),
                payloadIsJson(allOf(
                        withJsonPath("$.personInfoId", is(personInfoId.toString())),
                        withJsonPath("$.caseId", is(caseId)),
                        withJsonPath("$.personId", is(personId.toString()))
                ))))));
    }
}