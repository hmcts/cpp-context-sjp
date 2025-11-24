package uk.gov.moj.cpp.sjp.command.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.apache.activemq.artemis.utils.JsonLoader.createArrayBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import javax.json.Json;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.command.service.CaseService;
import uk.gov.moj.cpp.sjp.command.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.command.service.SessionService;
import uk.gov.moj.cpp.sjp.command.service.UserService;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
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
public class DecisionControllerTest {

    private static final UUID caseId = randomUUID();

    private static final UUID userId = randomUUID();

    private static final UUID sessionId = randomUUID();

    private ZonedDateTime savedAt = ZonedDateTime.now();

    private static final UUID WITHDRAWAL_REASON_ID_1 = randomUUID();

    private static final UUID WITHDRAWAL_REASON_ID_2 = randomUUID();

    private static final UUID REFERRAL_REASON_ID = randomUUID();

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

    @Mock
    private SessionService sessionService;

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

    private final JsonObject caseDetailsWithoutPostcode = createObjectBuilder()
            .add("id", caseId.toString())
            .add("urn", "TFL6754")
            .add("defendant", createObjectBuilder()
                    .add("id", randomUUID().toString())
                    .add("personalDetails", createObjectBuilder()
                            .add("address", createObjectBuilder()
                                    .add("address1", "1-43 Greenham Cl")
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

    private final JsonObjectBuilder referralReasonBuilder = createObjectBuilder()
            .add("id", REFERRAL_REASON_ID.toString())
            .add("seqId", 1)
            .add("reason", "Critical")
            .add("reasonCode", "PLR")
            .add("welshReason", "Ple amhendant")
            .add("welshSubReason","Diffynnydd i fynychu i gadarnhau ple")
            .add("hearingCode","CTL")
            .add("validFrom","2017-08-01")
            .add("validTo","2017-08-01");

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
        when(referenceDataService.getEnforcementArea(anyString())).thenReturn(of(enforcementArea));

        decisionController.saveDecision(saveDecisionCommand);
        verifySaveDecisionCommand(saveDecisionCommand);
    }

    @Test
    public void shouldHandleDecisionCommandWhenDefendantWithoutPostcode() {
        final JsonEnvelope saveDecisionCommand = createSaveDecisionCommand();
        when(userService.getCallingUserDetails(saveDecisionCommand)).thenReturn(userDetails);
        when(caseService.getCaseDetails(caseId.toString())).thenReturn(caseDetailsWithoutPostcode);

        decisionController.saveDecision(saveDecisionCommand);
        verifySaveDecisionCommand(saveDecisionCommand);
    }

    @Test
    public void shouldEnrichSaveDecisionCommandWithReferralReasonAndSubReason() {
        final JsonEnvelope saveDecisionCommand = createReferralSaveDecisionCommand();
        referralReasonBuilder.add("subReason","Defendant has to attend");
        when(userService.getCallingUserDetails(saveDecisionCommand)).thenReturn(userDetails);
        when(caseService.getCaseDetails(caseId.toString())).thenReturn(caseDetails);
        when(referenceDataService.getReferralReason(REFERRAL_REASON_ID.toString())).thenReturn(of(referralReasonBuilder.build()));

        decisionController.saveDecision(saveDecisionCommand);
        verifyReferralSaveDecisionCommand(saveDecisionCommand, "Critical (Defendant has to attend)");
    }

    @Test
    public void shouldEnrichSaveDecisionCommandWithReferralReasonOnly() {
        final JsonEnvelope saveDecisionCommand = createReferralSaveDecisionCommand();
        when(userService.getCallingUserDetails(saveDecisionCommand)).thenReturn(userDetails);
        when(caseService.getCaseDetails(caseId.toString())).thenReturn(caseDetails);
        when(referenceDataService.getReferralReason(REFERRAL_REASON_ID.toString())).thenReturn(of(referralReasonBuilder.build()));

        decisionController.saveDecision(saveDecisionCommand);
        verifyReferralSaveDecisionCommand(saveDecisionCommand, "Critical");
    }

    @Test
    public void shouldHandleAocpAcceptanceResponseTimeExpiredRequested() {
        assertThat(DecisionController.class, isHandlerClass(COMMAND_CONTROLLER)
                .with(method("handleAocpAcceptanceResponseTimeExpiredRequested").thatHandles("sjp.command.controller.expire-defendant-aocp-response-timer")));
    }

    @Test
    public void shouldHandleAocpAcceptanceResponseTimeExpiredRequestedWithoutDefendantPostCode() {
        final JsonEnvelope command = createAocpResponseTimeExpireRequestedCommand();
        when(userService.getCallingUserDetails(command)).thenReturn(userDetails);
        when(caseService.getCaseDetails(caseId.toString())).thenReturn(caseDetailsWithoutPostcode);

        decisionController.handleAocpAcceptanceResponseTimeExpiredRequested(command);
        verifyHandleAocpAcceptanceResponseTimeExpiredRequestedWithoutDefendant(command);
    }

    @Test
    public void shouldHandleAocpAcceptanceResponseTimeExpiredRequestedWithDefendantPostCode() {
        final JsonEnvelope command = createAocpResponseTimeExpireRequestedCommand();
        when(userService.getCallingUserDetails(command)).thenReturn(userDetails);
        when(caseService.getCaseDetails(caseId.toString())).thenReturn(caseDetails);
        when(referenceDataService.getEnforcementArea(anyString())).thenReturn(of(enforcementArea));

        decisionController.handleAocpAcceptanceResponseTimeExpiredRequested(command);
        verifyHandleAocpAcceptanceResponseTimeExpiredRequestedWithDefendant(command);
    }


    private void verifyReferralSaveDecisionCommand(final JsonEnvelope envelope, final String reasonForReferral) {
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> commandSent = envelopeCaptor.getValue();

        assertThat(commandSent.metadata(), withMetadataEnvelopedFrom(envelope).withName("sjp.command.save-decision"));

        assertThat(commandSent.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(allOf(
                withJsonPath("$.caseId", equalTo(envelope.payloadAsJsonObject().getString("caseId"))),
                withJsonPath("$.decisionId", notNullValue()),
                withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                withJsonPath("$.note", equalTo("case referred to court for review")),
                withJsonPath("$.savedAt", equalTo(savedAt.toString())),
                withJsonPath("$.savedBy", isJson(allOf(
                        withJsonPath("userId", equalTo(userId.toString())),
                        withJsonPath("firstName", is("John")),
                        withJsonPath("lastName", is("Smith"))
                ))),
                withJsonPath("$.offenceDecisions", hasSize(1)),
                withJsonPath("$.offenceDecisions[0].id", notNullValue()),
                withJsonPath("$.offenceDecisions[0].type", equalTo("REFER_FOR_COURT_HEARING")),
                withJsonPath("$.offenceDecisions[0].referralReasonId", equalTo(REFERRAL_REASON_ID.toString())),
                withJsonPath("$.offenceDecisions[0].referralReason", equalTo(reasonForReferral)

        ))));
    }

    private void verifySaveDecisionCommand(final JsonEnvelope envelope) {
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> commandSent = envelopeCaptor.getValue();

        assertThat(commandSent.metadata(), withMetadataEnvelopedFrom(envelope).withName("sjp.command.save-decision"));

        assertThat(commandSent.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(allOf(
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
                withJsonPath("$.offenceDecisions[0].id", notNullValue()),
                withJsonPath("$.offenceDecisions[0].offenceId", notNullValue()),
                withJsonPath("$.offenceDecisions[0].type", equalTo("WITHDRAWN")),
                withJsonPath("$.offenceDecisions[0].withdrawalReasonId", equalTo(WITHDRAWAL_REASON_ID_1.toString())),
                withJsonPath("$.offenceDecisions[1].id", notNullValue()),
                withJsonPath("$.offenceDecisions[1].offenceId", notNullValue()),
                withJsonPath("$.offenceDecisions[1].type", equalTo("WITHDRAWN")),
                withJsonPath("$.offenceDecisions[1].withdrawalReasonId", equalTo(WITHDRAWAL_REASON_ID_2.toString()))
        )));
    }

    private void verifyHandleAocpAcceptanceResponseTimeExpiredRequestedWithoutDefendant(final JsonEnvelope envelope) {
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> commandSent = envelopeCaptor.getValue();

        assertThat(commandSent.metadata(), withMetadataEnvelopedFrom(envelope).withName("sjp.command.expire-defendant-aocp-response-timer"));
        assertThat(commandSent.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(allOf(
                withJsonPath("$.caseId", equalTo(envelope.payloadAsJsonObject().getString("caseId"))),
                withJsonPath("$.decisionId", notNullValue()),
                withJsonPath("$.savedBy", isJson(allOf(
                        withJsonPath("userId", equalTo(userId.toString())),
                        withJsonPath("firstName", is("John")),
                        withJsonPath("lastName", is("Smith"))
                )))
        )));
    }

    private void verifyHandleAocpAcceptanceResponseTimeExpiredRequestedWithDefendant(final JsonEnvelope envelope) {
        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> commandSent = envelopeCaptor.getValue();

        assertThat(commandSent.metadata(), withMetadataEnvelopedFrom(envelope).withName("sjp.command.expire-defendant-aocp-response-timer"));
        assertThat(commandSent.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(allOf(
                withJsonPath("$.caseId", equalTo(envelope.payloadAsJsonObject().getString("caseId"))),
                withJsonPath("$.decisionId", notNullValue()),
                withJsonPath("$.savedBy", isJson(allOf(
                        withJsonPath("userId", equalTo(userId.toString())),
                        withJsonPath("firstName", is("John")),
                        withJsonPath("lastName", is("Smith"))
                ))),
                withJsonPath("$.defendant.court", isJson(allOf(
                        withJsonPath("nationalCourtCode", equalTo("1080")),
                        withJsonPath("nationalCourtName", is("Bedfordshire Magistrates' Court"))
                )))
        )));
    }


    private JsonEnvelope createSaveDecisionCommand() {
        final JsonObjectBuilder caseDecisionCommandBuilder = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("sessionId", sessionId.toString())
                .add("note", "wrongly convicted")
                .add("savedAt", savedAt.toString())
                .add("offenceDecisions", createOffenceDecision());

        return envelopeFrom(
                metadataWithRandomUUID("sjp.command.controller.save-decision")
                        .withUserId(userId.toString())
                , caseDecisionCommandBuilder.build()
        );
    }

    private JsonEnvelope createAocpResponseTimeExpireRequestedCommand() {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add("caseId", caseId.toString());

        return envelopeFrom(
                metadataWithRandomUUID("sjp.command.controller.expire-defendant-aocp-response-timer")
                        .withUserId(userId.toString())
                , builder.build()
        );
    }

    private JsonEnvelope createReferralSaveDecisionCommand() {
        final JsonObjectBuilder caseDecisionCommandBuilder = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("sessionId", sessionId.toString())
                .add("note", "case referred to court for review")
                .add("savedAt", savedAt.toString())
                .add("offenceDecisions", createReferToCourtOffenceDecision());

        return envelopeFrom(
                metadataWithRandomUUID("sjp.command.controller.save-decision")
                        .withUserId(userId.toString())
                , caseDecisionCommandBuilder.build()
        );
    }

    private static JsonArray createOffenceDecision() {
        final JsonArrayBuilder offenceDecisionBuilderArray = Json.createArrayBuilder();
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

    private static JsonArray createReferToCourtOffenceDecision() {
        final JsonArrayBuilder offenceDecisionBuilderArray = Json.createArrayBuilder();
        final JsonObjectBuilder offenceBuilder = createObjectBuilder();
        offenceBuilder.add("offenceId", randomUUID().toString());
        offenceBuilder.add("type", "REFER_FOR_COURT_HEARING");
        offenceBuilder.add("referralReasonId", REFERRAL_REASON_ID.toString());
        offenceDecisionBuilderArray.add(offenceBuilder);


        return offenceDecisionBuilderArray.build();
    }
}
