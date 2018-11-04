package uk.gov.moj.cpp.sjp.command.api;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildAddressWithPostcode;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder;

import java.util.Objects;
import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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

    private static final String SJP_COMMAND_DELETE_EMPLOYER = "sjp.command.delete-employer";
    private static final String SJP_COMMAND_UPDATE_EMPLOYER = "sjp.command.update-employer";

    @Spy
    @SuppressWarnings("unused")
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private EmployerApi employerApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldRenameUpdateCommandWithEmptyAddress() {
        updateCommandHelper(null, null);
    }

    @Test
    public void shouldRenameUpdateCommandWithPostcode() {
        updateCommandHelper(buildAddressWithPostcode("ec1a1bb"), buildAddressWithPostcode("EC1A 1BB"));
    }

    @Test
    public void shouldRenameDeleteCommand() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID("sjp.delete-employer")).build();

        employerApi.deleteEmployer(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(SJP_COMMAND_DELETE_EMPLOYER));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(command.payloadAsJsonObject()));
    }

    private void updateCommandHelper(final JsonObject providedAddress, final JsonObject expectedAddress) {
        final UUID caseId = UUID.randomUUID();
        final UUID defendantId = UUID.randomUUID();
        final String name = "employerName";

        final JsonEnvelopeBuilder commandBuilder = envelope().with(metadataWithRandomUUID("sjp.update-employer"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(defendantId, "defendantId")
                .withPayloadOf(name, "name");
        if (Objects.nonNull(providedAddress)) {
            commandBuilder.withPayloadOf(providedAddress, "address");
        }

        final JsonEnvelope command = commandBuilder.build();
        employerApi.updateEmployer(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(SJP_COMMAND_UPDATE_EMPLOYER));

        final JsonObjectBuilder expectedEmployerBuilder = createObjectBuilder()
                .add("name", name);

        if (Objects.nonNull(expectedAddress)) {
            expectedEmployerBuilder.add("address", expectedAddress);
        }

        final JsonObject expectedPayload = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("defendantId", defendantId.toString())
                .add("employer", expectedEmployerBuilder)
                .build();
        assertThat(newCommand.payloadAsJsonObject(), equalTo(expectedPayload));
    }

}
