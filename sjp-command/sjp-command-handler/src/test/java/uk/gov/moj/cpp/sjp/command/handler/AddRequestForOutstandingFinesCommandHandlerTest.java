package uk.gov.moj.cpp.sjp.command.handler;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloperWithEvents;
import static uk.gov.justice.services.test.utils.core.matchers.EventStreamMatcher.eventStreamAppendedWith;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeStreamMatcher.streamContaining;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.domain.annotation.Event;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.eventsourcing.source.core.EventSource;
import uk.gov.justice.services.eventsourcing.source.core.EventStream;
import uk.gov.justice.services.eventsourcing.source.core.exception.EventStreamException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.moj.cpp.sjp.event.OutstandingFinesRequested;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AddRequestForOutstandingFinesCommandHandlerTest {

    @Spy
    private final Enveloper enveloper = createEnveloperWithEvents(OutstandingFinesRequested.class);

    @InjectMocks
    private AddRequestForOutstandingFinesCommandHandler addRequestForOutstandingFinesCommandHandler;

    @Mock
    private EventStream eventStream;

    @Mock
    private EventSource eventSource;

    @Spy
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;

    @Spy
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Before
    public void setup() {
        setField(this.jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(this.objectToJsonObjectConverter, "mapper", new ObjectMapperProducer().objectMapper());
    }

    @Test
    public void computeOutstandingFines() throws EventStreamException {


        final JsonObject payload = createCourtRoomsOutstandingFInesQuery();

        final MetadataBuilder metadataBuilder = metadataWithRandomUUID("sjp.command.add-request-for-outstanding-fines");

        final JsonEnvelope envelope = JsonEnvelope.envelopeFrom(metadataBuilder, payload);

        when(eventSource.getStreamById(any())).thenReturn(eventStream);


        addRequestForOutstandingFinesCommandHandler.addRequestForOutstandingFines(envelope);

        assertThat(eventStream, eventStreamAppendedWith(
                streamContaining(
                        jsonEnvelope(
                                metadata().envelopedWith(envelope.metadata()).withName(OutstandingFinesRequested.class.getAnnotation(Event.class).value()),
                                payloadIsJson(
                                        withJsonPath("$.hearingDate", is("2020-03-26")))))));
    }

    private JsonObject createCourtRoomsOutstandingFInesQuery() {
        return Json.createObjectBuilder()
                .add("hearingDate", "2020-03-26")
                .build();
    }

}