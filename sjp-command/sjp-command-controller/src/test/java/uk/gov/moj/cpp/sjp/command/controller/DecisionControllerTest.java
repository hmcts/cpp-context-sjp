package uk.gov.moj.cpp.sjp.command.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.activemq.artemis.utils.JsonLoader.createArrayBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
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
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.command.service.CaseService;
import uk.gov.moj.cpp.sjp.command.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.command.service.UserService;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DecisionControllerTest {

    private static final UUID caseId = randomUUID();

    private static final UUID userId = randomUUID();

    private static final UUID sessionId = randomUUID();

    private ZonedDateTime savedAt = ZonedDateTime.now();

    private static final UUID WITHDRAWAL_REASON_ID_1 = randomUUID();

    private static final UUID WITHDRAWAL_REASON_ID_2 = randomUUID();

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private UserService userService;

    @Mock
    private CaseService caseService;

    @Mock
    private ReferenceDataService referenceDataService;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @InjectMocks
    private DecisionController decisionController;

    private final JsonObject userDetails = createObjectBuilder()
            .add("userId", userId.toString())
            .add("firstName", "John")
            .add("lastName", "Smith")
            .build();

    private final JsonObject caseDetails = createObjectBuilder()
            .add("id", caseId.toString())
            .add("urn", "TFL6754")
            .add("defendant", createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("personalDetails", createObjectBuilder()
                    .add("address", createObjectBuilder()
                        .add("postcode", "SE1 8HA")
                    )
                )
            ).build();

    private final JsonObject enforcementArea = createObjectBuilder()
            .add("enforcingCourtCode", 2222)
            .add("accountDivisionCode", 1111)
            .add("localJusticeArea", createObjectBuilder()
                .add("nationalCourtCode", "1080")
                .add("name", "Bedfordshire Magistrates' Court")
            ).build();

    @Test
    public void shouldHandleDecisionCommands() {
        assertThat(DecisionController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("saveDecision").thatHandles("sjp.command.controller.save-decision")));
    }

    @Test
    public void shouldEnrichSaveDecisionCommandWithUserDetailsAndDecisionId() {
        final JsonEnvelope saveDecisionCommand = createSaveDecisionCommand();
        when(userService.getCallingUserDetails(saveDecisionCommand)).thenReturn(userDetails);
        when(caseService.getCaseDetails(caseId.toString())).thenReturn(caseDetails);
        when(referenceDataService.getEnforcementArea(anyString())).thenReturn(enforcementArea);

        decisionController.saveDecision(saveDecisionCommand);
        verifySaveDecisionCommand(saveDecisionCommand);
    }

    private void verifySaveDecisionCommand(final JsonEnvelope envelope) {
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> commandSent = envelopeCaptor.getValue();

        assertThat(commandSent.metadata(), JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom(envelope).withName("sjp.command.save-decision"));

        assertThat(commandSent.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(anyOf(
                withJsonPath("$.caseId", equalTo(envelope.payloadAsJsonObject().getString("caseId"))),
                withJsonPath("$.decisionId", notNullValue()),
                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                withJsonPath("$.note", equalTo("wrongly convicted")),
                withJsonPath("$.savedAt", equalTo(savedAt.toString())),
                withJsonPath("$.savedBy", isJson(allOf(
                        withJsonPath("userId", equalTo(userId.toString())),
                        withJsonPath("firstName", is("John")),
                        withJsonPath("lastName", is("Smith"))
                ))),
                withJsonPath("$.offenceDecisions", hasSize(2)),
                withJsonPath("$.offenceDecisions[0].offenceDecisionId", notNullValue()),
                withJsonPath("$.offenceDecisions[0].offenceId", notNullValue()),
                withJsonPath("$.offenceDecisions[0].type", equalTo("WITHDRAWN")),
                withJsonPath("$.offenceDecisions[0].withdrawalReasonId", equalTo(WITHDRAWAL_REASON_ID_1.toString())),
                withJsonPath("$.offenceDecisions[1].offenceDecisionId", notNullValue()),
                withJsonPath("$.offenceDecisions[1].offenceId", notNullValue()),
                withJsonPath("$.offenceDecisions[1].type", equalTo("WITHDRAWN")),
                withJsonPath("$.offenceDecisions[1].withdrawalReasonId", equalTo(WITHDRAWAL_REASON_ID_2.toString()))
        )));
    }

    private JsonEnvelope createSaveDecisionCommand() {
        final JsonObjectBuilder caseDecisionCommandBuilder = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("sessionId", sessionId.toString())
                .add("note", "wrongly convicted")
                .add("savedAt", savedAt.toString())
                .add("offenceDecisions", createOffenceDecision());

        return envelopeFrom(metadataWithRandomUUID("sjp.command.controller.save-decision").withUserId(userId.toString()), caseDecisionCommandBuilder.build());
    }

    private static JsonArray createOffenceDecision() {
        final JsonArrayBuilder offenceDecisionBuilderArray = createArrayBuilder();
        final JsonObjectBuilder offenceBuilder1 = createObjectBuilder();
        offenceBuilder1.add("offenceId", randomUUID().toString());
        offenceBuilder1.add("type", "WITHDRAWN");
        offenceBuilder1.add("withdrawalReasonId", WITHDRAWAL_REASON_ID_1.toString());
        offenceDecisionBuilderArray.add(offenceBuilder1);

        final JsonObjectBuilder offenceBuilder2 = createObjectBuilder();
        offenceBuilder2.add("offenceId", randomUUID().toString());
        offenceBuilder2.add("type", "WITHDRAWN");
        offenceBuilder2.add("withdrawalReasonId", WITHDRAWAL_REASON_ID_2.toString());
        offenceDecisionBuilderArray.add(offenceBuilder2);

        return offenceDecisionBuilderArray.build();
    }
}
