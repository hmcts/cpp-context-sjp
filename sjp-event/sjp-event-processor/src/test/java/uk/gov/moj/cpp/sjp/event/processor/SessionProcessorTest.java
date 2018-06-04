package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.service.SchedulingService;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionEnded;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SessionProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    private SchedulingService schedulingService;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private SessionProcessor sessionProcessor;

    private final UUID existingSessionId = UUID.randomUUID();
    private final UUID newSessionId = UUID.randomUUID();
    private final String courtHouseName = "Wimbledon Magistrates' Court";
    private final String localJusticeAreaNationalCourtCode = "2577";
    private final String magistrate = "John Smith";

    @Before
    public void setUp() {
        when(schedulingService.getSession(eq(existingSessionId), any())).thenReturn(Optional.of(Json.createObjectBuilder().build()));
        when(schedulingService.getSession(AdditionalMatchers.not(eq(existingSessionId)), any())).thenReturn(Optional.empty());
    }

    @Test
    public void shouldStartMagistrateSessionInSchedulingAndEmitPublicSessionStartedEventWhenNewSessionIsCreated() {

        final JsonEnvelope magistrateSessionStartedEvent = envelopeFrom(metadataWithRandomUUID(MagistrateSessionStarted.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", newSessionId.toString())
                        .add("magistrate", magistrate)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .build());

        sessionProcessor.magistrateSessionStarted(magistrateSessionStartedEvent);

        verify(schedulingService).startMagistrateSession(magistrate, newSessionId, courtHouseName, localJusticeAreaNationalCourtCode, magistrateSessionStartedEvent);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(magistrateSessionStartedEvent).withName("public.sjp.session-started"),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(newSessionId.toString())),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.type", equalTo(MAGISTRATE.name()))
                )))));
    }

    @Test
    public void shouldStartDelegatedPowersSessionInSchedulingAndEmitPublicSessionStartedEventWhenNewSessionIsCreated() {

        final JsonEnvelope delegatedPowersSessionStartedEvent = envelopeFrom(metadataWithRandomUUID(DelegatedPowersSessionStarted.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", newSessionId.toString())
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .build());

        sessionProcessor.delegatedPowersSessionStarted(delegatedPowersSessionStartedEvent);

        verify(schedulingService).startDelegatedPowersSession(newSessionId, courtHouseName, localJusticeAreaNationalCourtCode, delegatedPowersSessionStartedEvent);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(delegatedPowersSessionStartedEvent).withName("public.sjp.session-started"),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(newSessionId.toString())),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.type", equalTo(DELEGATED_POWERS.name()))
                )))));
    }


    @Test
    public void shouldNotDoAnythingWhenExistingDelegatedPowersSessionIsMigrated() {
        final JsonEnvelope delegatedPowersSessionStartedEvent = envelopeFrom(metadataWithRandomUUID(DelegatedPowersSessionStarted.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", existingSessionId.toString())
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .build());

        sessionProcessor.delegatedPowersSessionStarted(delegatedPowersSessionStartedEvent);

        verify(schedulingService, never()).startDelegatedPowersSession(any(),any(),any(),any());
        verify(sender, never()).send(any());
    }

    @Test
    public void shouldNotDoAnythingWhenExistingMagistrateSessionIsMigrated() {
        final JsonEnvelope magistrateSessionStartedEvent = envelopeFrom(metadataWithRandomUUID(MagistrateSessionStarted.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", existingSessionId.toString())
                        .add("courtHouseName", courtHouseName)
                        .add("magistrate", magistrate)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .build());

        sessionProcessor.magistrateSessionStarted(magistrateSessionStartedEvent);

        verify(schedulingService, never()).startMagistrateSession(any(),any(),any(),any(),any());
        verify(sender, never()).send(any());
    }

    @Test
    public void shouldEndMagistrateSessionInScheduling() {
        final JsonEnvelope magistrateSessionEndedEvent = envelopeFrom(metadataWithRandomUUID(MagistrateSessionEnded.EVENT_NAME),
                createObjectBuilder().add("sessionId", existingSessionId.toString()).build());

        sessionProcessor.magistrateSessionEnded(magistrateSessionEndedEvent);

        verify(schedulingService).endSession(existingSessionId, magistrateSessionEndedEvent);
    }

    @Test
    public void shouldEndDelegatedPowersSessionInScheduling() {
        final JsonEnvelope delegatedPowersSessionEndedEvent = envelopeFrom(metadataWithRandomUUID(DelegatedPowersSessionEnded.EVENT_NAME),
                createObjectBuilder().add("sessionId", existingSessionId.toString()).build());

        sessionProcessor.magistrateSessionEnded(delegatedPowersSessionEndedEvent);

        verify(schedulingService).endSession(existingSessionId, delegatedPowersSessionEndedEvent);
    }

    @Test
    public void delegatedPowersSessionEndedTest() {
        final JsonEnvelope sessionEndedEvent = envelopeFrom(metadataWithRandomUUID(DelegatedPowersSessionEnded.EVENT_NAME),
                createObjectBuilder().add("sessionId", existingSessionId.toString()).build());

        sessionProcessor.delegatedPowersSessionEnded(sessionEndedEvent);

        verify(schedulingService).endSession(existingSessionId, sessionEndedEvent);
    }
}
