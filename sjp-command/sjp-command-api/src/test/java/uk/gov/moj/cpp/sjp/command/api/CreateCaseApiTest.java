package uk.gov.moj.cpp.sjp.command.api;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildAddressWithPostcode;
import static uk.gov.moj.cpp.sjp.command.utils.CommonObjectBuilderUtil.buildDefendantWithAddress;

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
public class CreateCaseApiTest {

    private static final String COMMAND_NAME = "sjp.create-sjp-case";
    private static final String NEW_COMMAND_NAME = "sjp.command.create-sjp-case";

    @Spy
    @SuppressWarnings("unused")
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Sender sender;

    @InjectMocks
    private CreateCaseApi createCaseApi;

    @Captor
    private ArgumentCaptor<JsonEnvelope> envelopeCaptor;

    @Test
    public void shouldHandleCommand() {
        assertThat(CreateCaseApi.class, isHandlerClass(COMMAND_API)
                .with(method("createSjpCase").thatHandles(COMMAND_NAME)));
    }

    @Test
    public void shouldRenameCommand() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID(COMMAND_NAME))
                .withPayloadOf(buildDefendantWithAddress(buildAddressWithPostcode("se11pj")), "defendant")
                .withPayloadOf(UUID.fromString("4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d"), "caseId")
                .withPayloadOf("TFL736699173", "urn")
                .build();

        final JsonObject transformedPayload = createObjectBuilder()
                .add("defendant", buildDefendantWithAddress(buildAddressWithPostcode("SE1 1PJ")))
                .add("caseId", "4ebc5dd1-9629-4b7d-a56b-efbcf0ec5e1d")
                .add("urn", "TFL736699173")
                .build();

        createCaseApi.createSjpCase(command);

        verify(sender).send(envelopeCaptor.capture());

        final JsonEnvelope newCommand = envelopeCaptor.getValue();
        assertThat(newCommand.metadata(), withMetadataEnvelopedFrom(command).withName(NEW_COMMAND_NAME));
        assertThat(newCommand.payloadAsJsonObject(), equalTo(transformedPayload));
    }


}
