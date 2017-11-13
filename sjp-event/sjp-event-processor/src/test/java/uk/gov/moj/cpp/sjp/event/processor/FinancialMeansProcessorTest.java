package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FinancialMeansProcessorTest {

    @InjectMocks
    private FinancialMeansProcessor processor;

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

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
                        .withName("public.structure.financial-means-updated"),
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
}