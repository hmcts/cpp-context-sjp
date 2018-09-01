package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SchedulingServiceTest {

    @Mock
    private Sender sender;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @InjectMocks
    private SchedulingService schedulingService;

    private final UUID sessionId = UUID.randomUUID();
    private final String courtHouseCode = "B01LY";
    private final String courtHouseName = "Wimbledon Magistrates' Court";
    private final String localJusticeAreaNationalCourtCode = "2577";
    private final String magistrate = "John Smith";
    private final JsonEnvelope queryEnvelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    @Test
    public void shouldSendCommandToStartMagistrateSession() {

        schedulingService.startMagistrateSession(magistrate, sessionId, courtHouseCode,
                courtHouseName, localJusticeAreaNationalCourtCode, queryEnvelope);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(queryEnvelope).withName("scheduling.command.start-sjp-session"),
                payloadIsJson(allOf(
                        withJsonPath("$.id", equalTo(sessionId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseCode)),
                        withJsonPath("$.courtLocation", equalTo(courtHouseName)),
                        withJsonPath("$.nationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withJsonPath("$.magistrate", equalTo(magistrate))
                )))));
    }

    @Test
    public void shouldSendCommandToStartDelegatedPowersSession() {

        schedulingService.startDelegatedPowersSession(sessionId, courtHouseCode,
                courtHouseName, localJusticeAreaNationalCourtCode, queryEnvelope);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(queryEnvelope).withName("scheduling.command.start-sjp-session"),
                payloadIsJson(allOf(
                        withJsonPath("$.id", equalTo(sessionId.toString())),
                        withJsonPath("$.courtHouseCode", equalTo(courtHouseCode)),
                        withJsonPath("$.courtLocation", equalTo(courtHouseName)),
                        withJsonPath("$.nationalCourtCode", equalTo(localJusticeAreaNationalCourtCode)),
                        withoutJsonPath("$.magistrate")
                )))));
    }

    @Test
    public void shouldSendCommandToEndSession() {

        schedulingService.endSession(sessionId, queryEnvelope);

        verify(sender).send(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(queryEnvelope).withName("scheduling.command.end-sjp-session"),
                payloadIsJson(withJsonPath("$.id", equalTo(sessionId.toString()))))));
    }

}
