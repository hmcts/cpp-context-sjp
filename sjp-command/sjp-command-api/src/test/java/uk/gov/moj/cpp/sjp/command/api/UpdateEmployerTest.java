package uk.gov.moj.cpp.sjp.command.api;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

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
public class UpdateEmployerTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private EmployerApi employerApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldRenameUpdateCommand() {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final String name = "employerName";
        final String address1 = "address1";

        final JsonEnvelope command = envelope().with(metadataWithRandomUUID("sjp.update-employer"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(defendantId, "defendantId")
                .withPayloadOf(name, "name")
                .withPayloadOf(address1, "address", "address1")
                .build();

        employerApi.updateEmployer(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName("sjp.command.update-employer"));

        final JsonObject expectedPayload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", defendantId.toString())
                .add("employer", createObjectBuilder()
                        .add("name", name)
                        .add("address", createObjectBuilder()
                                .add("address1", address1))
                )
                .build();
        assertThat(newCommand.payloadAsJsonObject(), equalTo(expectedPayload));
    }

    @Test
    public void shouldRenameDeleteCommand() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID("sjp.delete-employer")).build();

        employerApi.deleteEmployer(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName("sjp.command.delete-employer"));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

}
