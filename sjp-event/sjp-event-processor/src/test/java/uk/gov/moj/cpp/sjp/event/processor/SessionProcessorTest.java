package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.service.SchedulingService;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
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

    private final UUID sessionId = UUID.randomUUID();
    private final String courtHouseName = "Wimbledon Magistrates' Court";
    private final String localJusticeAreaNationalCourtCode = "2577";
    private final String magistrate = "John Smith";

    @Test
    public void shouldStartMagistrateSessionInSchedulingAndEmitPublicSessionStartedEvent() {

        final JsonEnvelope magistrateSessionStartedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.magistrate-session-started"),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("magistrate", magistrate)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .build());

        sessionProcessor.magistrateSessionStarted(magistrateSessionStartedEvent);

        verify(schedulingService).startMagistrateSession(magistrate, sessionId, courtHouseName, localJusticeAreaNationalCourtCode, magistrateSessionStartedEvent);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(magistrateSessionStartedEvent).withName("public.sjp.session-started"),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.type", equalTo(MAGISTRATE.name()))
                )))));
    }

    @Test
    public void shouldStartDelegatedPowersSessionInSchedulingAndEmitPublicSessionStartedEvent() {

        final JsonEnvelope delegatedPowersSessionStartedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.delegated-powers-session-started"),
                createObjectBuilder()
                        .add("sessionId", sessionId.toString())
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .build());

        sessionProcessor.delegatedPowersSessionStarted(delegatedPowersSessionStartedEvent);

        verify(schedulingService).startDelegatedPowersSession(sessionId, courtHouseName, localJusticeAreaNationalCourtCode, delegatedPowersSessionStartedEvent);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(delegatedPowersSessionStartedEvent).withName("public.sjp.session-started"),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(sessionId.toString())),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.type", equalTo(DELEGATED_POWERS.name()))
                )))));
    }

    @Test
    public void shouldEndMagistrateSessionInScheduling() {
        final JsonEnvelope magistrateSessionEndedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.magistrate-session-ended"),
                createObjectBuilder().add("sessionId", sessionId.toString()).build());

        sessionProcessor.magistrateSessionEnded(magistrateSessionEndedEvent);

        verify(schedulingService).endSession(sessionId, magistrateSessionEndedEvent);
    }

    @Test
    public void shouldEndDelegatedPowersSessionInScheduling() {
        final JsonEnvelope delegatedPowersSessionEndedEvent = envelopeFrom(metadataWithRandomUUID("sjp.events.delegated-powers-session-ended"),
                createObjectBuilder().add("sessionId", sessionId.toString()).build());

        sessionProcessor.magistrateSessionEnded(delegatedPowersSessionEndedEvent);

        verify(schedulingService).endSession(sessionId, delegatedPowersSessionEndedEvent);
    }
}
