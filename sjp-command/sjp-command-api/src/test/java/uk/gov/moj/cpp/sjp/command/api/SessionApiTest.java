package uk.gov.moj.cpp.sjp.command.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getAssignCaseGroups;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getEndAocpSessionGroups;
import static uk.gov.moj.cpp.sjp.command.api.accesscontrol.RuleConstants.getStartAocpSessionGroups;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;
import uk.gov.moj.cpp.sjp.command.api.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.domain.SessionCourt;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.JsonObject;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SessionApiTest extends BaseDroolsAccessControlTest {

    private static final String SJP_ASSIGN_NEXT_CASE = "sjp.assign-next-case";
    private static final String START_SESSION_COMMAND_NAME = "sjp.start-session";
    private static final String END_SESSION_COMMAND_NAME = "sjp.end-session";

    @Mock
    private ReferenceDataService referenceDataService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @Mock
    private Sender sender;

    @Captor
    private ArgumentCaptor<Envelope> envelopeArgumentCaptor;

    @InjectMocks
    private SessionApi sessionApi;

    private UUID sessionId = UUID.randomUUID();

    private UUID caseId = UUID.randomUUID();

    private UUID userId = UUID.randomUUID();

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public SessionApiTest() {
        super("COMMAND_API_SESSION");
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedStartSession() {
        final Action action = createActionFor(START_SESSION_COMMAND_NAME);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getStartAocpSessionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthorisedEndSession() {
        final Action action = createActionFor(END_SESSION_COMMAND_NAME);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getEndAocpSessionGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldAllowAuthoriseAssignNextCase() {
        final Action action = createActionFor(SJP_ASSIGN_NEXT_CASE);
        given(userAndGroupProvider.isMemberOfAnyOfTheSuppliedGroups(action, getAssignCaseGroups()))
                .willReturn(true);
        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
    }

    @Test
    public void shouldEnhanceAndRenameStartSessionCommand() {

        final String courtHouseOUCode = "B01OK";

        final JsonEnvelope startSessionCommand = envelope().with(metadataWithRandomUUID(START_SESSION_COMMAND_NAME))
                .withPayloadOf(sessionId.toString(), "sessionId")
                .withPayloadOf(courtHouseOUCode, "courtHouseOUCode")
                .withPayloadOf("Jay", "magistrate")
                .withPayloadOf(Json.createObjectBuilder().add("userId", userId.toString()).build(), "legalAdviser")
                .withPayloadOf(Json.createArrayBuilder().add("P1").add("P2").build(), "prosecutors")
                .build();

        final SessionCourt sessionCourt = new SessionCourt("Wimbledon Magistrates' Court", "2577");

        when(referenceDataService.getCourtByCourtHouseOUCode(courtHouseOUCode, startSessionCommand)).thenReturn(Optional.of(sessionCourt));

        sessionApi.startSession(startSessionCommand);

        verify(sender).send(argThat(jsonEnvelope(withMetadataEnvelopedFrom(startSessionCommand).withName("sjp.command.start-session"),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseOUCode)),
                        withJsonPath("$.courtHouseName", equalTo(sessionCourt.getCourtHouseName())),
                        withJsonPath("$.magistrate", equalTo("Jay")),
                        withJsonPath("$.legalAdviser.userId", equalTo(userId.toString())),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(sessionCourt.getLocalJusticeAreaNationalCourtCode())),
                        withJsonPath("$.prosecutors[0]", equalTo("P1")),
                        withJsonPath("$.prosecutors[1]", equalTo("P2"))
                )))));
    }

    @Test
    public void shouldThrowExceptionWhenCourtHouseDoesNotExist() {

        final String courtHouseOUCode = "B01OK";

        final JsonEnvelope startSessionCommand = envelope().with(metadataWithRandomUUID(START_SESSION_COMMAND_NAME))
                .withPayloadOf(sessionId.toString(), "sessionId")
                .withPayloadOf(courtHouseOUCode, "courtHouseOUCode")
                .build();

        when(referenceDataService.getCourtByCourtHouseOUCode(courtHouseOUCode, startSessionCommand)).thenReturn(Optional.empty());

        var e = assertThrows(BadRequestException.class, () -> sessionApi.startSession(startSessionCommand));
        assertThat(e.getMessage(), is(String.format("Court house with ou code %s not found", courtHouseOUCode)));
    }

    @Test
    public void shouldRenameEndSessionCommand() {

        final JsonEnvelope endSessionCommand = envelope().with(metadataWithRandomUUID(END_SESSION_COMMAND_NAME))
                .withPayloadOf(sessionId.toString(), "sessionId")
                .build();

        sessionApi.endSession(endSessionCommand);

        verify(sender).send(argThat(jsonEnvelope(withMetadataEnvelopedFrom(endSessionCommand).withName("sjp.command.end-session"),
                payloadIsJson(withJsonPath("$.sessionId", equalTo(sessionId.toString()))))));
    }

    @Test
    public void endSessionShouldValidateSessionIdIsNotNull() {
        final JsonObject payload = Json.createObjectBuilder().addNull("sessionId").build();
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(END_SESSION_COMMAND_NAME))
                .withPayloadFrom(payload)
                .build();

        var e = assertThrows(BadRequestException.class, () -> sessionApi.endSession(envelope));
        assertThat(e.getMessage(), CoreMatchers.is("Invalid sessionId provided"));
    }

    @Test
    public void endSessionShouldValidateSessionIdIsNotInTheWrongFormat() {
        final JsonEnvelope envelope = envelope().with(metadataWithRandomUUID(END_SESSION_COMMAND_NAME))
                .withPayloadOf("invalid UUID", "sessionId")
                .build();

        var e = assertThrows(BadRequestException.class, () -> sessionApi.endSession(envelope));
        assertThat(e.getMessage(), CoreMatchers.is("Invalid sessionId provided"));
    }

    @Test
    public void shouldRenameAssignNextCaseCommand() {
        final JsonEnvelope assignCaseCommand = envelope().with(metadataWithRandomUUID(SJP_ASSIGN_NEXT_CASE))
                .withPayloadOf(sessionId.toString(), "sessionId")
                .build();

        sessionApi.assignNextCase(assignCaseCommand);

        verify(sender).send(argThat(jsonEnvelope(withMetadataEnvelopedFrom(assignCaseCommand).withName("sjp.command.assign-next-case"),
                payloadIsJson(withJsonPath("$.sessionId", equalTo(sessionId.toString()))))));
    }

    @Test
    public void shouldRenameAssignCaseCommand() {
        final JsonEnvelope assignCaseCommand = envelope().with(metadataWithRandomUUID("sjp.assign-case"))
                .withPayloadOf(caseId.toString(), "caseId")
                .withPayloadOf(userId.toString(), "userId")
                .build();

        sessionApi.assignCase(assignCaseCommand);

        verify(sender).send(argThat(jsonEnvelope(withMetadataEnvelopedFrom(assignCaseCommand).withName("sjp.command.controller.assign-case"),
                payloadIsJson(allOf(
                        withJsonPath("$.caseId", equalTo(caseId.toString())),
                        withJsonPath("$.userId", equalTo(userId.toString()))
                )))));
    }

    @Test
    public void shouldRenameUnassignCaseCommand() {
        final JsonEnvelope unassignCaseCommand = envelope().with(metadataWithRandomUUID("sjp.unassign-case"))
                .withPayloadOf(caseId.toString(), "caseId")
                .build();

        sessionApi.unassignCase(unassignCaseCommand);

        verify(sender).send(argThat(jsonEnvelope(withMetadataEnvelopedFrom(unassignCaseCommand).withName("sjp.command.unassign-case"),
                payloadIsJson(withJsonPath("$.caseId", equalTo(caseId.toString()))))));
    }

    @Test
    public void shouldHandleSessionCommands() {
        assertThat(SessionApi.class, isHandlerClass(COMMAND_API)
                .with(allOf(
                        method("startSession").thatHandles(START_SESSION_COMMAND_NAME),
                        method("endSession").thatHandles(END_SESSION_COMMAND_NAME),
                        method("assignNextCase").thatHandles(SJP_ASSIGN_NEXT_CASE),
                        method("assignCase").thatHandles("sjp.assign-case"),
                        method("unassignCase").thatHandles("sjp.unassign-case")
                )));
    }

    @Test
    public void shouldHandleResetAocpSessionRequest() {
        assertThat(SessionApi.class, isHandlerClass(COMMAND_API)
                .with(method("handleResetAocpSessionRequest").thatHandles("sjp.reset-aocp-session")));
    }

    @Test
    public void shouldResetSessionCommand() {
        final JsonEnvelope command = envelope().with(metadataWithRandomUUID("sjp.reset-aocp-session")).build();

        sessionApi.handleResetAocpSessionRequest(command);
        verify(sender).send(envelopeArgumentCaptor.capture());

        final Envelope newCommand = envelopeArgumentCaptor.getValue();
        assertThat(newCommand.metadata().name(), is("sjp.command.reset-aocp-session"));
    }
}
