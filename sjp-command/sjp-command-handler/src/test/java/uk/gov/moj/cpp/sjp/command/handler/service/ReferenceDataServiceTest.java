package uk.gov.moj.cpp.sjp.command.handler.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import java.util.Optional;
import java.util.UUID;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.json.JsonValue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ReferenceDataServiceTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    @Mock
    private JsonObjectToObjectConverter converter = new JsonObjectToObjectConverter();

    @InjectMocks
    private ReferenceDataService referenceDataService;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), JsonObjects.createObjectBuilder().build());

    @Test
    public void shouldReturnEnforcementAreaByPostcode() {
        final String postcode = "CRO 2GE";
        final JsonObject enforcementArea = newEnforcementArea();

        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), enforcementArea);

        when(enforcementAreaQueryByPostcode(postcode)).thenReturn(queryResponse);

        final Optional<JsonObject> actualEnforcementArea = referenceDataService.getEnforcementAreaByPostcode(postcode, envelope);

        assertThat(actualEnforcementArea.isPresent(), equalTo(true));
        assertThat(actualEnforcementArea.get(), equalTo(enforcementArea));
    }

    @Test
    public void shouldReturnEmptyEnforcementAreaIfNotFoundByPostcode() {
        final String postcode = "CR0 2GE";
        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL);

        when(enforcementAreaQueryByPostcode(postcode)).thenReturn(queryResponse);

        final Optional<JsonObject> actualEnforcementArea = referenceDataService.getEnforcementAreaByPostcode(postcode, envelope);

        assertThat(actualEnforcementArea.isPresent(), equalTo(false));
    }

    @Test
    public void shouldReturnEnforcementAreaByLocalJusticeAreaNationalCourtCode() {
        final String localJusticeAreaNationalCourtCode = "1000";
        final JsonObject enforcementArea = newEnforcementArea();

        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), enforcementArea);

        when(enforcementAreaQueryByNationalCourtCode(localJusticeAreaNationalCourtCode)).thenReturn(queryResponse);

        final Optional<JsonObject> actualEnforcementArea = referenceDataService.getEnforcementAreaByLocalJusticeAreaNationalCourtCode(localJusticeAreaNationalCourtCode, envelope);

        assertThat(actualEnforcementArea.isPresent(), equalTo(true));
        assertThat(actualEnforcementArea.get(), equalTo(enforcementArea));
    }

    @Test
    public void shouldReturnEmptyEnforcementAreaIfNotFoundByLocalJusticeAreaNationalCourtCode() {
        final String localJusticeAreaNationalCourtCode = "1000";
        final JsonEnvelope enforcementArea = envelopeFrom(metadataWithRandomUUIDAndName(), JsonValue.NULL);

        when(enforcementAreaQueryByNationalCourtCode(localJusticeAreaNationalCourtCode)).thenReturn(enforcementArea);

        final Optional<JsonObject> actualEnforcementArea = referenceDataService.getEnforcementAreaByLocalJusticeAreaNationalCourtCode(localJusticeAreaNationalCourtCode, envelope);

        assertThat(actualEnforcementArea.isPresent(), equalTo(false));
    }

    @Test
    public void shouldReturnProsecutor() {
        final UUID id = randomUUID();
        final JsonObject prosecutorJson = JsonObjects.createObjectBuilder()
                .add("prosecutorCode", "100")
                .add("prosecutorName", "test").build();

        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), prosecutorJson);

        when(requester.request(any(JsonEnvelope.class))).thenReturn(queryResponse);

        final Optional<JsonObject> prosecutor = referenceDataService.getProsecutor(envelope, id);

        assertThat(prosecutor.isPresent(), equalTo(true));
        assertThat(prosecutor.get(), equalTo(prosecutorJson));
    }

    private JsonEnvelope enforcementAreaQueryByPostcode(final String postcode) {
        return requester.request(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.enforcement-area.v2"),
                payloadIsJson(withJsonPath("$.postcode", equalTo(postcode))))));
    }

    private JsonEnvelope enforcementAreaQueryByNationalCourtCode(final String localJusticeAreaNationalCourtCode) {
        return requester.request(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.enforcement-area.v2"),
                payloadIsJson(withJsonPath("$.localJusticeAreaNationalCourtCode", equalTo(localJusticeAreaNationalCourtCode))))));
    }

    private static JsonObject newEnforcementArea() {
        return JsonObjects.createObjectBuilder()
                .add("accountDivisionCode", "100")
                .add("enforcingCourtCode", "1000")
                .add("nationalPaymentPhone", "030 0790 9901")
                .add("paymentQueriesAddress1", "London Collection and Compliance Centre")
                .add("paymentQueriesAddress2", "HMCTS")
                .add("paymentQueriesAddress3", "PO Box 31090")
                .add("paymentQueriesAddress4", "London")
                .add("paymentQueriesPostcode", "SW1P 3WQ")
                .add("remittanceAdviceEmailAddress", "lcccBank@hmcts.gsi.gov.uk")
                .build();
    }
}