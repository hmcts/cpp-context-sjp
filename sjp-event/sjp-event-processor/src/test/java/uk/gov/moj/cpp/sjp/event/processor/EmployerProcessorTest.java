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
public class EmployerProcessorTest {

    @InjectMocks
    private EmployerProcessor processor;

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Test
    public void publishes() {
        final UUID defendantId = UUID.randomUUID();
        final String employerReference = "employer reference";
        final String employerName = "name";
        final String employerPhone = "020 7998 9300";
        final String address1 = "address1";
        final String address2 = "address2";
        final String address3 = "address3";
        final String address4 = "address4";
        final String postCode = "postCode";

        final JsonEnvelope privateEventEnvelope = envelopeFrom(
                metadataWithRandomUUID("sjp.events.employer-updated"),
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .add("employeeReference", employerReference)
                        .add("name", employerName)
                        .add("phone", employerPhone)
                        .add("address",
                                createObjectBuilder()
                                        .add("address1", address1)
                                        .add("address2", address2)
                                        .add("address3", address3)
                                        .add("address4", address4)
                                        .add("postCode", postCode)
                        )
                        .build()
        );

        processor.updateEmployer(privateEventEnvelope);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(privateEventEnvelope)
                        .withName("public.structure.employer-updated"),
                payloadIsJson(allOf(
                        withJsonPath("$.defendantId", is(defendantId.toString())),
                        withJsonPath("$.employeeReference", is(employerReference)),
                        withJsonPath("$.name", is(employerName)),
                        withJsonPath("$.phone", is(employerPhone)),
                        withJsonPath("$.address.address1", is(address1)),
                        withJsonPath("$.address.address2", is(address2)),
                        withJsonPath("$.address.address3", is(address3)),
                        withJsonPath("$.address.address4", is(address4)),
                        withJsonPath("$.address.postCode", is(postCode))
                ))
        )));
    }
}