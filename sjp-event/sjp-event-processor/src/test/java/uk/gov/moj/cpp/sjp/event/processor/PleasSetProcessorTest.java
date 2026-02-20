package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.PleasSet;

import java.io.StringReader;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PleasSetProcessorTest {

    @InjectMocks
    private PleasSetProcessor processor;

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @Test
    public void raisesPleasSetPublicEvent() throws JsonProcessingException {
        final UUID caseId = UUID.randomUUID();

        final PleasSet pleasSet = new PleasSet(caseId, null,
                singletonList(new Plea(UUID.randomUUID(), UUID.randomUUID(), PleaType.GUILTY)));


        final JsonEnvelope envelope = EnvelopeFactory.createEnvelope("sjp.events.pleas-set",
                convertToJsonObject(pleasSet));

        processor.publishPleasSet(envelope);

        final Plea plea = pleasSet.getPleas().get(0);
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> sentEnvelope = envelopeCaptor.getValue();
        assertThat(sentEnvelope.metadata(), withMetadataEnvelopedFrom(envelope).withName("public.sjp.pleas-set"));


        assertThat(sentEnvelope.payload(), payloadIsJson(
                allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.pleas[0].defendantId", equalTo(plea.getDefendantId().toString())),
                        withJsonPath("$.pleas[0].offenceId", equalTo(plea.getOffenceId().toString())),
                        withJsonPath("$.pleas[0].pleaType", equalTo(plea.getPleaType().toString()))
                )
                )
        );
    }

    public JsonObject convertToJsonObject(PleasSet pleasSet) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(pleasSet);
        return Json.createReader(new StringReader(json)).readObject();
    }
}
