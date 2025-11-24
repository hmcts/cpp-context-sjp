package uk.gov.moj.cpp.sjp.event.processor;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.session.DelegatedPowersSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.MagistrateSessionStarted;
import uk.gov.moj.cpp.sjp.event.session.ResetAocpSession;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SessionProcessorTest {

    @Mock
    private Sender sender;

    @Mock
    SjpService sjpService;

    @Captor
    private ArgumentCaptor<Envelope> envelopeCaptor;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private SessionProcessor sessionProcessor;

    private final UUID existingSessionId = UUID.randomUUID();
    private final UUID newSessionId = UUID.randomUUID();
    private final String courtHouseCode = "B01LY";
    private final String courtHouseName = "Wimbledon Magistrates' Court";
    private final String localJusticeAreaNationalCourtCode = "2577";
    private final String magistrate = "John Smith";
    private static final String AOCP_COURT_HOUSE_CODE = "B52CM00";
    private static final String AOCP_COURT_HOUSE_NAME = "Bristol Magistrates' Court";
    private static final String AOCP_COURT_LJA = "1450";

    @Test
    public void shouldStartMagistrateSessionAndEmitPublicSessionStartedEventWhenNewSessionIsCreated() {

        final JsonEnvelope magistrateSessionStartedEvent = envelopeFrom(metadataWithRandomUUID(MagistrateSessionStarted.EVENT_NAME), magistrateSessionStartedEventPayload(newSessionId));

        sessionProcessor.magistrateSessionStarted(magistrateSessionStartedEvent);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(magistrateSessionStartedEvent).withName("public.sjp.session-started"),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(newSessionId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseCode)),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.type", equalTo(MAGISTRATE.name()))
                )))));
    }

    @Test
    public void shouldStartDelegatedPowersSessionEmitPublicSessionStartedEventWhenNewSessionIsCreated() {

        final JsonEnvelope delegatedPowersSessionStartedEvent = envelopeFrom(metadataWithRandomUUID(DelegatedPowersSessionStarted.EVENT_NAME),
                createObjectBuilder()
                        .add("sessionId", newSessionId.toString())
                        .add("courtHouseCode", courtHouseCode)
                        .add("courtHouseName", courtHouseName)
                        .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                        .build());

        sessionProcessor.delegatedPowersSessionStarted(delegatedPowersSessionStartedEvent);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(delegatedPowersSessionStartedEvent).withName("public.sjp.session-started"),
                payloadIsJson(allOf(
                        withJsonPath("$.sessionId", equalTo(newSessionId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseCode)),
                        withJsonPath("$.courtHouseName", equalTo(courtHouseName)),
                        withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.type", equalTo(DELEGATED_POWERS.name()))
                )))));
    }

    @Test
    public void shouldHandleAocpSessionResetRequestedForActiveSession() {

        final String sessionId = UUID.randomUUID().toString();

        final JsonEnvelope resetAocpSessionEnvelope = envelopeFrom(metadataWithRandomUUID(ResetAocpSession.EVENT_NAME),
                createObjectBuilder().add("resetAt", ZonedDateTime.now().toString()).build());

        JsonObject sessionResponse = createObjectBuilder().add("sessionId", sessionId).build();

        when(sjpService.getLatestAocpSessionDetails(any())).thenReturn(sessionResponse);

        sessionProcessor.aocpSessionResetRequested(resetAocpSessionEnvelope);

        verify(sender, times(2)).send(envelopeCaptor.capture());

        final List<Envelope> envelope= envelopeCaptor.getAllValues();

        assertThat(envelope.size(), is(2));

        assertThat(envelope.get(0).metadata().name(), is("sjp.command.end-session"));
        final JsonObject sessionEndpayload = (JsonObject) envelope.get(0).payload();
        assertThat(sessionEndpayload.getString("sessionId"), is(sessionId));

        assertThat(envelope.get(1).metadata().name(), is("sjp.command.start-session"));
        final JsonObject sessionStartpayload = (JsonObject) envelope.get(1).payload();
        assertThat(sessionStartpayload.getString("sessionId"), notNullValue());
        assertThat(sessionStartpayload.getString("courtHouseName"), is(AOCP_COURT_HOUSE_NAME));
        assertThat(sessionStartpayload.getString("courtHouseCode"), is(AOCP_COURT_HOUSE_CODE));
        assertThat(sessionStartpayload.getString("localJusticeAreaNationalCourtCode"), is(AOCP_COURT_LJA));
        assertThat(sessionStartpayload.getBoolean("isAocpSession"), is(true));
        assertThat(sessionStartpayload.getJsonArray("prosecutors").getString(0), is("TFL"));
        assertThat(sessionStartpayload.getJsonArray("prosecutors").getString(1), is("TVL"));
        assertThat(sessionStartpayload.getJsonArray("prosecutors").getString(2), is("DVLA"));
    }

    @Test
    public void shouldHandleAocpSessionResetRequestedForNonActiveSession() {
        final JsonEnvelope resetAocpSessionEnvelope = envelopeFrom(metadataWithRandomUUID(ResetAocpSession.EVENT_NAME),
                createObjectBuilder().add("resetAt", ZonedDateTime.now().toString()).build());

        when(sjpService.getLatestAocpSessionDetails(any())).thenReturn(null);

        sessionProcessor.aocpSessionResetRequested(resetAocpSessionEnvelope);

        verify(sender).send(envelopeCaptor.capture());

        final Envelope envelope= envelopeCaptor.getValue();

        assertThat(envelope.metadata().name(), is("sjp.command.start-session"));
        final JsonObject sessionStartpayload = (JsonObject) envelope.payload();
        assertThat(sessionStartpayload.getString("sessionId"), notNullValue());
        assertThat(sessionStartpayload.getString("courtHouseName"), is(AOCP_COURT_HOUSE_NAME));
        assertThat(sessionStartpayload.getString("courtHouseCode"), is(AOCP_COURT_HOUSE_CODE));
        assertThat(sessionStartpayload.getString("localJusticeAreaNationalCourtCode"), is(AOCP_COURT_LJA));
        assertThat(sessionStartpayload.getBoolean("isAocpSession"), is(true));
        assertThat(sessionStartpayload.getJsonArray("prosecutors").getString(0), is("TFL"));
        assertThat(sessionStartpayload.getJsonArray("prosecutors").getString(1), is("TVL"));
        assertThat(sessionStartpayload.getJsonArray("prosecutors").getString(2), is("DVLA"));

    }

    private JsonObject magistrateSessionStartedEventPayload(final UUID sessionId) {
        return createObjectBuilder()
                .add("sessionId", sessionId.toString())
                .add("magistrate", magistrate)
                .add("courtHouseCode", courtHouseCode)
                .add("courtHouseName", courtHouseName)
                .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                .build();
    }
}
