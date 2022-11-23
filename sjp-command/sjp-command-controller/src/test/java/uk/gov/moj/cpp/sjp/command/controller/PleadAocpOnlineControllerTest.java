package uk.gov.moj.cpp.sjp.command.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.TestFixtures.getEnforcementAreaPayload;
import static uk.gov.moj.cpp.sjp.command.TestFixtures.getLocalJusticeAreasPayload;
import static uk.gov.moj.cpp.sjp.command.controller.PleadAocpOnlineController.COMMAND_NAME;

import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.command.service.ReferenceDataService;

import javax.json.JsonObject;
import javax.json.JsonValue;

import com.jayway.jsonpath.ReadContext;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PleadAocpOnlineControllerTest {

    private final String POSTCODE = "W1T 1JY";
    private final String NATIONAL_COURTCODE = "2574";
    private final String userId = randomUUID().toString();
    private final String CASE_ID = randomUUID().toString();
    private final String DEF_ID = randomUUID().toString();

    private final JsonObject enforcementAreaPayload = getEnforcementAreaPayload(828, NATIONAL_COURTCODE);
    private final JsonObject pleadOnlinePayload = getPayload();

    @Mock
    private Sender sender;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private PleadAocpOnlineController pleadAocpOnlineController;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    @Before
    public void before() {
        when(referenceDataService.getEnforcementArea(POSTCODE)).thenReturn(of(enforcementAreaPayload));
    }

    public void after() {
        reset(referenceDataService);
    }

    private JsonObject getPayload() {
        final JsonObject address = createObjectBuilder()
                .add("postcode", POSTCODE)
                .build();
        final JsonObject personalDetails = createObjectBuilder()
                .add("firstName", "X")
                .add("dateOfBirth", "01/01/1700")
                .add("address", address)
                .build();

        return createObjectBuilder()
                .add("caseId", CASE_ID)
                .add("defendantId", DEF_ID)
                .add("personalDetails", personalDetails)
                .build();
    }

    @Test
    public void testEnrichedPayload() {
        //given
        final Metadata metadata = metadataWithRandomUUID(COMMAND_NAME).withUserId(userId).build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, pleadOnlinePayload);
        final String REGION = "LONDON";
        final JsonObject localJusticeAreasPayload = getLocalJusticeAreasPayload(REGION, NATIONAL_COURTCODE);
        when(referenceDataService.getLocalJusticeAreas(NATIONAL_COURTCODE)).thenReturn(of(localJusticeAreasPayload));

        //when
        pleadAocpOnlineController.pleadOnline(jsonEnvelope);

        //Then
        final Matcher<? super ReadContext> newPayloadMatcher = allOf(
                withJsonPath("$.caseId", equalTo(CASE_ID)),
                withJsonPath("$.personalDetails", notNullValue()),
                withJsonPath("$.personalDetails.region", equalTo(REGION)));

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> command = envelopeCaptor.getValue();
        assertThat(command.metadata(), JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom(jsonEnvelope).withName(COMMAND_NAME));
        assertThat(command.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(newPayloadMatcher));
    }

    @Test
    public void testEnrichedPayloadWhenRegionIsNull() {
        //given
        final Metadata metadata = metadataWithRandomUUID(COMMAND_NAME).withUserId(userId).build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, pleadOnlinePayload);
        final JsonObject localJusticeAreasPayload = getLocalJusticeAreasPayload(null, NATIONAL_COURTCODE);
        when(referenceDataService.getLocalJusticeAreas(NATIONAL_COURTCODE)).thenReturn(of(localJusticeAreasPayload));

        //when
        pleadAocpOnlineController.pleadOnline(jsonEnvelope);

        //Then
        final Matcher<? super ReadContext> newPayloadMatcher = allOf(
                withJsonPath("$.caseId", equalTo(CASE_ID)),
                withJsonPath("$.personalDetails", notNullValue()),
                not(hasJsonPath("$.personalDetails.region")));

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> command = envelopeCaptor.getValue();
        assertThat(command.metadata(), JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom(jsonEnvelope).withName(COMMAND_NAME));
        assertThat(command.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(newPayloadMatcher));

    }
}
