package uk.gov.moj.cpp.sjp.command.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.command.api.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.domain.SessionCourt;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionApiTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private ReferenceDataService referenceDataService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Spy
    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

    @Mock
    private Sender sender;

    @InjectMocks
    private SessionApi sessionApi;

    private UUID sessionId = UUID.randomUUID();

    private UUID caseId = UUID.randomUUID();

    private UUID userId = UUID.randomUUID();

    @Test
    public void shouldEnhanceAndRenameStartSessionCommand() {

        final String courtHouseOUCode = "B01OK";

        final JsonEnvelope startSessionCommand = envelope().with(metadataWithRandomUUID("sjp.start-session"))
                .withPayloadOf(sessionId.toString(), "sessionId")
                .withPayloadOf(courtHouseOUCode, "courtHouseOUCode")
                .build();

        final SessionCourt sessionCourt = new SessionCourt("Wimbledon Magistrates' Court", "2577");

        when(referenceDataService.getCourtByCourtHouseOUCode(courtHouseOUCode, startSessionCommand)).thenReturn(Optional.of(sessionCourt));

        sessionApi.startSession(startSessionCommand);

        verify(sender).send(argThat(jsonEnvelope(withMetadataEnvelopedFrom(startSessionCommand).withName("sjp.command.start-session"),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseOUCode)),
                        withJsonPath("$.courtHouseName", equalTo(sessionCourt.getCourtHouseName())),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(sessionCourt.getLocalJusticeAreaNationalCourtCode()))
                )))));
    }

    @Test
    public void shouldThrowExceptionWhenCourtHouseDoesNotExist() {

        final String courtHouseOUCode = "B01OK";

        final JsonEnvelope startSessionCommand = envelope().with(metadataWithRandomUUID("sjp.start-session"))
                .withPayloadOf(sessionId.toString(), "sessionId")
                .withPayloadOf(courtHouseOUCode, "courtHouseOUCode")
                .build();

        thrown.expect(BadRequestException.class);
        thrown.expectMessage(String.format("Court house with ou code %s not found", courtHouseOUCode));

        when(referenceDataService.getCourtByCourtHouseOUCode(courtHouseOUCode, startSessionCommand)).thenReturn(Optional.empty());

        sessionApi.startSession(startSessionCommand);
    }

    @Test
    public void shouldRenameEndSessionCommand() {

        final JsonEnvelope endSessionCommand = envelope().with(metadataWithRandomUUID("sjp.end-session"))
                .withPayloadOf(sessionId.toString(), "sessionId")
                .build();

        sessionApi.endSession(endSessionCommand);

        verify(sender).send(argThat(jsonEnvelope(withMetadataEnvelopedFrom(endSessionCommand).withName("sjp.command.end-session"),
                payloadIsJson(withJsonPath("$.sessionId", equalTo(sessionId.toString()))))));
    }


    @Test
    public void shouldRenameAssignNextCaseCommand() {
        final JsonEnvelope assignCaseCommand = envelope().with(metadataWithRandomUUID("sjp.assign-next-case"))
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
                        method("startSession").thatHandles("sjp.start-session"),
                        method("endSession").thatHandles("sjp.end-session"),
                        method("assignNextCase").thatHandles("sjp.assign-next-case"),
                        method("assignCase").thatHandles("sjp.assign-case"),
                        method("unassignCase").thatHandles("sjp.unassign-case")
                )));
    }

}
