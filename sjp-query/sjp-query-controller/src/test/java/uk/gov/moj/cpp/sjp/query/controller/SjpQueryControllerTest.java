package uk.gov.moj.cpp.sjp.query.controller;

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

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.sjp.query.controller.converter.CaseConverter;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpQueryControllerTest {

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private CaseConverter caseConverter;

    @InjectMocks
    private SjpQueryController sjpQueryController;

    @Test
    public void shouldHandlesQueries() {
        assertThat(SjpQueryController.class, isHandlerClass(Component.QUERY_CONTROLLER)
                .with(allOf(
                        method("findCaseByUrn").thatHandles("sjp.query.case-by-urn").withRequesterPassThrough(),
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
                        method("getTransparencyReportMetadata").thatHandles("sjp.query.transparency-report-metadata").withRequesterPassThrough()
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
        when(requester.request(any(JsonEnvelope.class))).thenReturn(validUrn ? caseDetails : envelope().withNullPayload().build());

        JsonValue resultPayload = JsonValue.NULL;
        if (validUrn && validPostcode) {
            final JsonObject objectToReturn = createObjectBuilder().build();
            resultPayload = objectToReturn;
            when(caseConverter.addOffenceReferenceDataToOffences(caseDetails.payloadAsJsonObject(), query)).thenReturn(objectToReturn);
        }

        final JsonEnvelope result = sjpQueryController.findCaseByUrnPostcode(query);

        verify(requester).request(argThat(jsonEnvelope(metadata().withName("sjp.query.case-by-urn-postcode"),
                payloadIsJson(withJsonPath("$.urn", equalTo(urn))))));

        if (validUrn && validPostcode) {
            verify(caseConverter).addOffenceReferenceDataToOffences(caseDetails.payloadAsJsonObject(), query);
        }

        assertThat(result.metadata().name(), equalTo("sjp.query.case-by-urn-postcode"));
        assertThat(result.payload(), equalTo(resultPayload));
    }

}
