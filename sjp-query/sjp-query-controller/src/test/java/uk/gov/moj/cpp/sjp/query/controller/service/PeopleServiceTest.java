package uk.gov.moj.cpp.sjp.query.controller.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PeopleServiceTest {

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private Requester requester;

    @InjectMocks
    private PeopleService peopleService;

    private String personId;

    private JsonEnvelope request;

    @Before
    public void setup() {
        personId = randomUUID().toString();
        request = envelope()
                .with(metadataWithRandomUUID("sjp.query.case-by-urn-postcode"))
                .withPayloadOf(personId, "personId").build();
    }

    @Test
    public void shouldGetPerson() {
        final JsonEnvelope response = envelope().build();
        when(requester.request(any(JsonEnvelope.class))).thenReturn(response);

        final JsonValue result = peopleService.getPerson(personId, request);

        assertThat(result, is(response.payload()));

        verify(requester).request(argThat(jsonEnvelope(metadata().withName("people.query.person"),
                payloadIsJson(withJsonPath("$.personId", equalTo(personId))))));
    }

    @Test
    public void shouldAddPersonInfoForDefendantWithMatchingPostcode() {
        shouldPostcodeOfPerson(true);
    }

    @Test
    public void shouldReturnNullIfPostcodeDoesNotMatch() {
        shouldPostcodeOfPerson(false);
    }

    private void shouldPostcodeOfPerson(final boolean postcodeMatch) {
        final String postcode = "CR0 1YG";
        final JsonObject address = createObjectBuilder().add("postCode", postcode).build();
        final JsonEnvelope response = envelope()
                .withPayloadOf(personId, "id")
                .withPayloadOf("FIRST_NAME", "firstName")
                .withPayloadOf("LAST_NAME", "lastName")
                .withPayloadOf("DATE_OF_BIRTH", "dateOfBirth")
                .withPayloadOf("HOME_TELEPHONE", "homeTelephone")
                .withPayloadOf("MOBILE", "mobile")
                .withPayloadOf("EMAIL", "email")
                .withPayloadOf("NATIONAL_INSURANCE_NUMBER", "nationalInsuranceNumber")
                .withPayloadOf(address, "address")
                .build();
        when(requester.request(any(JsonEnvelope.class))).thenReturn(response);

        final JsonObject defendant = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("personId", personId)
                .add("offences", createArrayBuilder().build())
                .build();
        final JsonObject caseDetails = createObjectBuilder()
                .add("id", randomUUID().toString())
                .add("defendants", createArrayBuilder()
                        .add(defendant)
                        .build())
                .build();

        final JsonObject result = (JsonObject) peopleService.addPersonInfoForDefendantWithMatchingPostcode(postcodeMatch ? postcode : "INVALID", caseDetails, request);

        if (postcodeMatch) {
            assertThat(result.getString("id"), is(caseDetails.getString("id")));
            final JsonObject resultDefendant = result.getJsonArray("defendants").getValuesAs(JsonObject.class).get(0);
            assertThat(resultDefendant.getString("id"), is(defendant.getString("id")));
            assertThat(resultDefendant.getJsonObject("person"), is(response.payloadAsJsonObject()));
            assertThat(resultDefendant.getJsonArray("offences"), is(defendant.getJsonArray("offences")));
        } else {
            assertThat(result, nullValue());
        }
    }
}
