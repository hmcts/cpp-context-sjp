package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteDocsStartedProcessorTest {

    private static final UUID CASE_ID = randomUUID();
    private static final UUID DEFENDANT_ID = randomUUID();
    private static final String FINANCIAL_MEANS_DELETE_DOCS_STARTED = "sjp.events.financial-means-delete-docs-started";

    @InjectMocks
    private DeleteDocsStartedProcessor deleteDocsStartedProcessor;

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> jsonEnvelopeCaptor;

    @BeforeEach
    public void setUp() {
        final JsonObjectToObjectConverter jsonObjectToObjectConverter = new JsonObjectToObjectConverter();
        setField(jsonObjectToObjectConverter, "objectMapper", new ObjectMapperProducer().objectMapper());
        setField(deleteDocsStartedProcessor, "jsonObjectToObjectConverter", jsonObjectToObjectConverter);
    }

    @Test
    public void shouldGenerateAllOffencesWithdrawnOrDismissedEvent() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("defendantId", DEFENDANT_ID.toString())
                .build();
        final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUID(FINANCIAL_MEANS_DELETE_DOCS_STARTED), payload);


        deleteDocsStartedProcessor.handleDeleteDocsStarted(envelope);

        verify(sender).send(jsonEnvelopeCaptor.capture());

        final Envelope<JsonValue> publicEventEnvelope = jsonEnvelopeCaptor.getValue();

        assertThat((publicEventEnvelope.metadata().name()), is("public.sjp.all-offences-for-defendant-dismissed-or-withdrawn"));
        assertThat(publicEventEnvelope.payload(), payloadIsJson(allOf(withJsonPath("caseId", is(CASE_ID.toString())))));
        assertThat(publicEventEnvelope.payload(), payloadIsJson(allOf(withJsonPath("defendantId", is(DEFENDANT_ID.toString())))));

    }

}
