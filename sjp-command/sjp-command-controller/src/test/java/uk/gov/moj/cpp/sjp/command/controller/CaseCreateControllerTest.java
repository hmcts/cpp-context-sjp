package uk.gov.moj.cpp.sjp.command.controller;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.command.TestFixtures.getAddressPayload;
import static uk.gov.moj.cpp.sjp.command.TestFixtures.getEnforcementAreaPayload;
import static uk.gov.moj.cpp.sjp.command.TestFixtures.getLocalJusticeAreasPayload;
import static uk.gov.moj.cpp.sjp.command.controller.CaseCreateController.COMMAND_NAME;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class CaseCreateControllerTest {
    @Mock
    private Sender sender;

    @Mock
    private ReferenceDataService referenceDataService;

    @InjectMocks
    private CaseCreateController caseCreateController;

    @Captor
    private ArgumentCaptor<Envelope<JsonValue>> envelopeCaptor;

    private final String DEFENDANT_ID = randomUUID().toString();
    private final String ID = randomUUID().toString();
    private final String POSTCODE = "W1T 1JY";
    private final String NATIONAL_COURTCODE = "2574";
    private final String userId = randomUUID().toString();

    private final JsonObject enforcementAreaPayload = getEnforcementAreaPayload(828, NATIONAL_COURTCODE);

    private final JsonObject defendant = createObjectBuilder()
            .add("id", DEFENDANT_ID)
            .add("address", getAddressPayload(POSTCODE))
            .build();

    private final JsonObject payload = createObjectBuilder()
            .add("id", ID)
            .add("defendant", defendant)
            .build();

    @BeforeEach
    public void before() {
        when(referenceDataService.getEnforcementArea(POSTCODE)).thenReturn(of(enforcementAreaPayload));
    }

    public void after() {
        reset(referenceDataService);
    }

    @Test
    public void testEnrichedPayload() {
        //given
        final Metadata metadata = metadataWithRandomUUID(COMMAND_NAME).withUserId(userId).build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);
        final String REGION = "ESSEX";
        final JsonObject localJusticeAreasPayload = getLocalJusticeAreasPayload(REGION, NATIONAL_COURTCODE);
        when(referenceDataService.getLocalJusticeAreas(NATIONAL_COURTCODE)).thenReturn(of(localJusticeAreasPayload));

        //when
        caseCreateController.createSjpCase(jsonEnvelope);

        //Then
        final Matcher<? super ReadContext> newPayloadMatcher = allOf(
                withJsonPath("$.defendant.id", equalTo(DEFENDANT_ID)),
                withJsonPath("$.defendant.region", equalTo(REGION)));

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> command = envelopeCaptor.getValue();
        assertThat(command.metadata(), JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom(jsonEnvelope).withName(COMMAND_NAME));
        assertThat(command.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(newPayloadMatcher));
    }

    @Test
    public void testEnrichedPayloadWhenRegionIsNull() {
        //given
        final Metadata metadata = metadataWithRandomUUID(COMMAND_NAME).withUserId(userId).build();
        final JsonEnvelope jsonEnvelope = envelopeFrom(metadata, payload);
        final JsonObject localJusticeAreasPayload = getLocalJusticeAreasPayload(null, NATIONAL_COURTCODE);
        when(referenceDataService.getLocalJusticeAreas(NATIONAL_COURTCODE)).thenReturn(of(localJusticeAreasPayload));

        //when
        caseCreateController.createSjpCase(jsonEnvelope);

        //Then
        final Matcher<? super ReadContext> newPayloadMatcher = allOf(
                withJsonPath("$.defendant.id", equalTo(DEFENDANT_ID)),
                not(hasJsonPath("$.defendant.region")));

        verify(sender).send(envelopeCaptor.capture());
        final Envelope<JsonValue> command = envelopeCaptor.getValue();
        assertThat(command.metadata(), JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom(jsonEnvelope).withName(COMMAND_NAME));
        assertThat(command.payload(), JsonEnvelopePayloadMatcher.payloadIsJson(newPayloadMatcher));

    }
}
