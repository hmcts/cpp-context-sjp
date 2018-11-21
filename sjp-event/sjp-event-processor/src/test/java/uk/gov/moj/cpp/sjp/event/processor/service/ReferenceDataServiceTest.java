package uk.gov.moj.cpp.sjp.event.processor.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUIDAndName;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelopeFrom;

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

    private final JsonEnvelope envelope = envelopeFrom(metadataWithRandomUUIDAndName(), Json.createObjectBuilder().build());

    @Test
    public void shouldReturnCountryForAPostcode(){
        final String actualCountry = "Wales";
        final String outwardCode = "WA11";
        final JsonObject responsePayload = Json.createObjectBuilder().add("country", "Wales").build();
        final JsonEnvelope queryResponse = envelopeFrom(metadataWithRandomUUIDAndName(), responsePayload);
        when(requestCountryByPostcode(outwardCode)).thenReturn(queryResponse);
        final String country = referenceDataService.getCountryByPostcode(outwardCode, envelope);
        assertThat(actualCountry, is(country));
    }

    private JsonEnvelope requestCountryByPostcode(final String postCode) {
        return requester.requestAsAdmin(argThat(jsonEnvelope(
                withMetadataEnvelopedFrom(envelope).withName("referencedata.query.country-by-postcode"),
                payloadIsJson(withJsonPath("$.postCode", equalTo(postCode))))));
    }

}
