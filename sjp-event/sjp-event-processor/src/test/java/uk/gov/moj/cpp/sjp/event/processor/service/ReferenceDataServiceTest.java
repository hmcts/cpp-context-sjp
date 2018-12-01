package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

import uk.gov.justice.json.schemas.domains.sjp.ProsecutingAuthority;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import javax.json.Json;
import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReferenceDataServiceTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private ReferenceDataService referenceDataService;

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), createObjectBuilder().build());

    @Test
    public void shouldReturnCountryForAPostcode() {
        final String actualCountry = "Wales";
        final String outwardCode = "WA11";
        final JsonObject responsePayload = createObjectBuilder().add("country", "Wales").build();
        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), responsePayload);
        when(requestCountryByPostcode(outwardCode)).thenReturn(queryResponse);
        final String country = referenceDataService.getCountryByPostcode(outwardCode, envelope);
        assertThat(actualCountry, is(country));
    }

    @Test
    public void shouldReturnProsecutor() {
        final JsonObject responsePayload = createObjectBuilder().add("id", "prosecutorId").build();
        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedata.query.prosecutors"),
                responsePayload);
        when(requestProsecutor(ProsecutingAuthority.TFL)).thenReturn(queryResponse);

        final JsonObject prosecutor = referenceDataService.getProsecutor(ProsecutingAuthority.TFL, envelope);
        assertThat(prosecutor, is(responsePayload));
    }

    @Test
    public void shouldReturnReferralReasons() {
        final JsonObject responsePayload = createObjectBuilder().add("referralReasons", Json.createArrayBuilder()).build();
        final JsonEnvelope queryResponse = envelopeFrom(
                metadataWithRandomUUID("referencedata.query.referral-reasons"),
                responsePayload);

        when(requestReferralReasons()).thenReturn(queryResponse);

        final JsonObject referralReasons = referenceDataService.getReferralReasons(envelope);
        assertThat(referralReasons, is(responsePayload));
    }

    private Object requestReferralReasons() {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.referral-reasons"),
                payloadIsJson(notNullValue()))));
    }

    private Object requestProsecutor(final ProsecutingAuthority prosecutingAuthority) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.prosecutors"),
                payloadIsJson(withJsonPath("$.prosecutorCode", equalTo(prosecutingAuthority.name()))))));
    }

    private JsonEnvelope requestCountryByPostcode(final String postCode) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.country-by-postcode"),
                payloadIsJson(withJsonPath("$.postCode", equalTo(postCode))))));
    }

}
