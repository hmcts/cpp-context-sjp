package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory;

import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class FinancialMeansProcessorTest {

    @InjectMocks
    private FinancialMeansProcessor processor;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope<JsonObject>> envelopeCaptor;

    @Test
    public void publishes() {
        final UUID defendantId = UUID.randomUUID();
        final int incomeAmount = 100;
        final String incomeFrequency = "MONTHLY";
        final boolean benefitsClaimed = true;
        final String benefitsType = "free text describing benefits type";
        final String employmentStatus = "EMPLOYED";

        final JsonEnvelope privateEventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.financial-means-updated"),
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .add("income", createObjectBuilder()
                                .add("amount", incomeAmount)
                                .add("frequency", incomeFrequency)
                        )
                        .add("benefits", createObjectBuilder()
                                .add("claimed", benefitsClaimed)
                                .add("type", benefitsType)
                        )
                        .add("employmentStatus", employmentStatus)
                        .build()
        );

        processor.updateFinancialMeans(privateEventEnvelope);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(privateEventEnvelope)
                        .withName("public.sjp.financial-means-updated"),
                payloadIsJson(allOf(
                        withJsonPath("$.defendantId", is(defendantId.toString())),
                        withJsonPath("$.income.amount", is(incomeAmount)),
                        withJsonPath("$.income.frequency", is(incomeFrequency)),
                        withJsonPath("$.benefits.claimed", is(benefitsClaimed)),
                        withJsonPath("$.benefits.type", is(benefitsType)),
                        withJsonPath("$.employmentStatus", is(employmentStatus))
                ))
        )));
    }

    @Test
    public void shouldDeleteDefendantFinancialMeansWithoutAnyMaterials() {

        //Preparing the Response Payload and Envelope
        final String defendantId = "50770ba9-37ea-4713-8cab-fe5bf1202716";
        final JsonObject requestPayload = createObjectBuilder()
                .add("defendantId", defendantId).build();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("sjp.events.financial-means-deleted"),
                requestPayload);

        processor.deleteFinancialMeans(requestMessage);
        verify(sender, times(1)).send(envelopeCaptor.capture());

        //Asserting on all the expected responses at a payload level....
        List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        Envelope<JsonObject> publicEventSenderEnvelope = commands.get(0);
        assertThat(publicEventSenderEnvelope.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.sjp.events.defendant-financial-means-deleted"));
        assertEquals(defendantId, publicEventSenderEnvelope.payload().getString("defendantId"));


    }

    @Test
    public void shouldDeleteDefendantFinancialMeans() {

        //Preparing the Response Payload and Envelope
        final String defendantId = "50770ba9-37ea-4713-8cab-fe5bf1202716";
        final JsonObject requestPayload = createObjectBuilder()
                .add("defendantId", defendantId)
                .add("materialIds", createArrayBuilder().add("M001").add("M002")).build();

        final JsonEnvelope requestMessage = JsonEnvelope.envelopeFrom(
                MetadataBuilderFactory.metadataWithRandomUUID("sjp.events.financial-means-deleted"),
                requestPayload);

        processor.deleteFinancialMeans(requestMessage);
        verify(sender, times(3)).send(envelopeCaptor.capture());

        List<Envelope<JsonObject>> commands = envelopeCaptor.getAllValues();
        checkMaterialPayloadSent(requestMessage, commands.get(0), "M001");
        checkMaterialPayloadSent(requestMessage, commands.get(1), "M002");

        Envelope<JsonObject> publicEventSenderEnvelope = commands.get(2);
        assertThat(publicEventSenderEnvelope.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("public.sjp.events.defendant-financial-means-deleted"));


    }

    private void checkMaterialPayloadSent(final JsonEnvelope requestMessage,
                                          final Envelope<JsonObject> materialContextCommandSenderEnvelope,
                                          final String materialId) {
        assertThat(materialContextCommandSenderEnvelope.metadata(),
                withMetadataEnvelopedFrom(requestMessage).withName("material.command.delete-material"));
        JsonObject jsonObject = materialContextCommandSenderEnvelope.payload();
        assertEquals(materialId, jsonObject.getString("materialId"));
    }
}