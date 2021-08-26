package uk.gov.moj.cpp.sjp.command.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AddFinancialImpositionAccountNumberBdfApiTest {

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<JsonEnvelope> dispatchedCommand;

    @InjectMocks
    private AddFinancialImpositionAccountNumberBdfApi accountNumberApi;

    @Test
    public void shouldHandleAddFinancialImpositionAccountNumber() {
        assertThat(AddFinancialImpositionAccountNumberBdfApi.class, isHandlerClass(COMMAND_API)
                .with(method("addFinancialImpositionAccountNumber").thatHandles("sjp.add-financial-imposition-account-number-bdf")));
    }

    @Test
    public void shouldHandleAndDispatchCommand() {
        final JsonEnvelope command = createAddAccountNumberCommand();

        final JsonObject payload = command.payloadAsJsonObject();

        accountNumberApi.addFinancialImpositionAccountNumber(command);

        verify(sender).send(dispatchedCommand.capture());
        final JsonEnvelope dispatchedEnvelope = dispatchedCommand.getValue();
        assertThat(dispatchedEnvelope.metadata().name(),
                is("sjp.command.add-financial-imposition-account-number-bdf"));

        assertThat(dispatchedEnvelope.payloadAsJsonObject(), payloadIsJson(allOf(
                withJsonPath("$.caseId", equalTo(payload.getString("caseId"))),
                withJsonPath("$.correlationId", equalTo(payload.getString("correlationId"))),
                withJsonPath("$.accountNumber", equalTo(payload.getString("accountNumber")))
        )));

    }

    private JsonEnvelope createAddAccountNumberCommand() {
        final JsonObject payload = createObjectBuilder()
                .add("caseId", randomUUID().toString())
                .add("correlationId", randomUUID().toString())
                .add("accountNumber", "88237111A")
                .build();

        return envelopeFrom(
                metadataWithRandomUUID("sjp.add-financial-imposition-account-number-bdf"),
                payload);
    }

}
