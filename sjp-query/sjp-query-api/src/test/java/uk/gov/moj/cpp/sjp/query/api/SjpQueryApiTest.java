package uk.gov.moj.cpp.sjp.query.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import uk.gov.justice.services.core.annotation.Component;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.query.api.converter.CaseConverter;
import uk.gov.moj.cpp.sjp.query.api.validator.SjpQueryApiValidator;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class SjpQueryApiTest {

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();


    @Mock
    private SjpQueryApiValidator sjpQueryApiValidator;

    @Mock
    private CaseConverter caseConverter;

    @InjectMocks
    private SjpQueryApi sjpQueryApi;

    @Test
    public void shouldHandlesQueries() {
        assertThat(SjpQueryApi.class, isHandlerClass(Component.QUERY_API)
                .with(allOf(
                        method("findCase").thatHandles("sjp.query.case"),
                        method("findCaseByUrn").thatHandles("sjp.query.case-by-urn").withRequesterPassThrough(),
                        method("findCaseByUrnPostcode").thatHandles("sjp.query.case-by-urn-postcode"),
                        method("findFinancialMeans").thatHandles("sjp.query.financial-means").withRequesterPassThrough(),
                        method("findEmployer").thatHandles("sjp.query.employer").withRequesterPassThrough(),
                        method("findCaseSearchResults").thatHandles("sjp.query.case-search-results").withRequesterPassThrough(),
                        method("findCasesMissingSjpn").thatHandles("sjp.query.cases-missing-sjpn").withRequesterPassThrough(),
                        method("searchCaseByMaterialId").thatHandles("sjp.query.cases-search-by-material-id").withRequesterPassThrough(),
                        method("getResultOrders").thatHandles("sjp.query.result-orders").withRequesterPassThrough(),
                        method("getReadyCases").thatHandles("sjp.query.ready-cases").withRequesterPassThrough(),
                        method("getCaseAssignment").thatHandles("sjp.query.case-assignment").withRequesterPassThrough(),
                        method("getProsecutingAuthority").thatHandles("sjp.query.case-prosecuting-authority").withRequesterPassThrough(),
                        method("getDefendantDetailsUpdates").thatHandles("sjp.query.defendant-details-updates").withRequesterPassThrough(),
                        method("getCaseNotes").thatHandles("sjp.query.case-notes").withRequesterPassThrough(),
                        method("getOffencesVerdicts").thatHandles("sjp.query.offence-verdicts"),
                        method("getTransparencyReportMetadata").thatHandles("sjp.query.transparency-report-metadata"),
                        method("getPressTransparencyReportMetadata").thatHandles("sjp.query.press-transparency-report-metadata"),
                        method("getOffencesVerdicts").thatHandles("sjp.query.offence-verdicts"),
                        method("getCaseNotes").thatHandles("sjp.query.case-notes").withRequesterPassThrough(),
                        method("getOutstandingFines").thatHandles("sjp.query.defendant-outstanding-fines").withRequesterPassThrough()
                )));
    }

    @Test
    public void shouldFindCaseByUrnPostcode() {
        testFindCaseByUrnPostcode(true, true);
    }

    @Test
    public void shouldNotFindCaseByUrnPostcode_invalidUrn() {
        testFindCaseByUrnPostcode(false, true); // doesn't matter that validPostcode could also be false
    }

    @Test
    public void shouldNotFindCaseByUrnPostcode_invalidPostcode() {
        testFindCaseByUrnPostcode(true, false);
    }

    private void testFindCaseByUrnPostcode(final boolean validUrn, final boolean validPostcode) {
        final String urn = "ABC123";
        final String postcode = "CR0 1YG";
        final JsonEnvelope query = envelope()
                .with(metadataWithRandomUUID("sjp.query.case-by-urn-postcode"))
                .withPayloadOf(urn, "urn")
                .withPayloadOf(postcode, "postcode").build();

        final JsonObject address = createObjectBuilder().add("postcode", postcode).build();
        final JsonObject personalDetails = createObjectBuilder().add("address", address).build();
        final JsonObject defendant = createObjectBuilder().add("personalDetails", personalDetails).build();

        final JsonEnvelope caseDetails = envelope()
                .withPayloadOf(randomUUID().toString(), "id")
                .withPayloadOf(defendant, "defendant")
                .build();

        when(requester.request(any(JsonEnvelope.class))).thenReturn(caseDetails);
        when(sjpQueryApiValidator.validateCasePostConviction(any(JsonObject.class))).thenReturn(Collections.emptyMap());

        JsonValue resultPayload = JsonValue.NULL;

        if (validUrn && validPostcode) {
            final JsonObject objectToReturn = createObjectBuilder().build();
            resultPayload = objectToReturn;
            when(caseConverter.addOffenceReferenceDataToOffences(caseDetails.payloadAsJsonObject(), query)).thenReturn(objectToReturn);
        }

        final JsonEnvelope result = sjpQueryApi.findCaseByUrnPostcode(query);

        verify(requester).request(argThat(jsonEnvelope(metadata().withName("sjp.query.case-by-urn-postcode"),
                payloadIsJson(withJsonPath("$.urn", equalTo(urn))))));

//        if (validUrn && validPostcode) {
//            verify(caseConverter).addOffenceReferenceDataToOffences(caseDetails.payloadAsJsonObject(), query);
//        }

//        assertThat(result.metadata().name(), equalTo("sjp.query.case-by-urn-postcode"));
        assertThat(result.payload(), equalTo(resultPayload));
    }
}
