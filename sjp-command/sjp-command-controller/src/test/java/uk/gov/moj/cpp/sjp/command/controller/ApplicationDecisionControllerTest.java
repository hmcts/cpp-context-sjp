package uk.gov.moj.cpp.sjp.command.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.command.service.UserService;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationDecisionControllerTest {

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private UserService userService;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @InjectMocks
    private ApplicationDecisionController decisionController;

    private static final UUID USER_ID = randomUUID();

    private static final UUID CASE_ID = randomUUID();

    private static final UUID APPLICATION_ID = randomUUID();

    private static final UUID SESSION_ID = randomUUID();

    private final JsonObject userDetails = createObjectBuilder()
            .add("userId", USER_ID.toString())
            .add("firstName", "John")
            .add("lastName", "Smith")
            .build();

    @Test
    public void shouldHandleApplicationDecisionCommands() {
        assertThat(ApplicationDecisionController.class, isHandlerClass(COMMAND_CONTROLLER)
            .with(method("saveApplicationDecision")
                    .thatHandles("sjp.command.controller.save-application-decision"))
        );
    }

    @Test
    public void shouldEnrichSaveApplicationDecisionWithUserDetails() {
        final JsonEnvelope command = createSaveApplicationDecisionCommand();
        when(userService.getCallingUserDetails(command)).thenReturn(userDetails);
        decisionController.saveApplicationDecision(command);
        verifySaveApplicationDecisionCommand(command);
    }

    private void verifySaveApplicationDecisionCommand(final JsonEnvelope command) {
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> commandSent = envelopeCaptor.getValue();
        assertThat(commandSent.metadata().name(), equalTo("sjp.command.handler.save-application-decision"));
        assertThat(commandSent.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(anyOf(
                withJsonPath("$.caseId", equalTo(CASE_ID.toString())),
                withJsonPath("$.applicationId", equalTo(APPLICATION_ID.toString())),
                withJsonPath("$.sessionId", equalTo(SESSION_ID.toString())),
                withJsonPath("$.granted", equalTo(true)),
                withJsonPath("$.outOfTime", equalTo(false)),
                withJsonPath("$.savedBy", isJson(allOf(
                        withJsonPath("userId", equalTo(USER_ID.toString())),
                        withJsonPath("firstName", is("John")),
                        withJsonPath("lastName", is("Smith"))
                ))))));
    }

    private JsonEnvelope createSaveApplicationDecisionCommand() {
        final JsonObjectBuilder decisionBuilder = createObjectBuilder()
                .add("caseId", CASE_ID.toString())
                .add("applicationId", APPLICATION_ID.toString())
                .add("sessionId", SESSION_ID.toString())
                .add("granted", true)
                .add("outOfTime", false);

        return envelopeFrom(
                metadataWithRandomUUID("sjp.command.controller.save-application-decision")
                    .withUserId(USER_ID.toString()),
                decisionBuilder.build()
        );
    }

}
