package uk.gov.moj.cpp.sjp.query.controller;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.spi.DefaultEnvelopeProvider;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher;
import uk.gov.moj.cpp.sjp.query.controller.response.DefendantProfilingView;

import javax.json.JsonObject;
import java.time.LocalDate;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher.isHandlerClass;
import static uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher.method;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;

@RunWith(MockitoJUnitRunner.class)
public class SjpQueryControllerTest {

    @Mock
    private Requester requester;

    @Mock
    private JsonEnvelope query;

    @Mock
    private JsonEnvelope response;

    @Spy
    private final Enveloper enveloper = EnveloperFactory.createEnveloper();

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
                                                             method("getTransparencyReportMetadata").thatHandles("sjp.query.transparency-report-metadata").withRequesterPassThrough(),
                                                             method("getCourtExtract").thatHandles("sjp.query.case-court-extract").withRequesterPassThrough()
                                                     )));
    }

    @Test
    public void shouldFindDefendantOustandingFines() {

        final DefendantProfilingView profilingView = DefendantProfilingView.newBuilder()
                                                       .withFirstName("firstName")
                                                       .withLastName("lastName")
                                                       .withId(randomUUID())
                                                       .build();
        testGetOutstandingFines(profilingView);

    }

    @Test
    public void shouldFindDefendantOustandingFines_withOptionalInputs() {

        final DefendantProfilingView profilingView = DefendantProfilingView.newBuilder()
                                                       .withFirstName("firstName")
                                                       .withLastName("lastName")
                                                       .withDateOfBirth(LocalDate.parse("1991-04-05"))
                                                       .withNationalInsuranceNumber("SS-123213-123")
                                                       .withId(randomUUID())
                                                       .build();
        testGetOutstandingFines(profilingView);

    }

    private void testGetOutstandingFines(final DefendantProfilingView profilingView) {
        final ArgumentCaptor<JsonEnvelope> jsonEnvelopeCaptor = ArgumentCaptor.forClass(JsonEnvelope.class);
        final Metadata metadata = metadataOf(
                randomUUID(), "sjp.query.defendant-profile"
        ).build();
        final JsonEnvelope query = envelopeFrom(metadata,
                createObjectBuilder()
                        .add("defendantId", randomUUID().toString())
                        .build());

        when(requester.request(any(JsonEnvelope.class), eq(DefendantProfilingView.class))).thenReturn(
                new DefaultEnvelopeProvider().envelopeFrom(metadata, profilingView)
        );

        when(requester.requestAsAdmin(jsonEnvelopeCaptor.capture())).thenReturn(response);
        final JsonObject jsonObject = createObjectBuilder()
                                        .add("outstandingFines", createArrayBuilder()
                                                                         .add(createObjectBuilder().add("name", "name")))
                                        .build();
        when(response.payloadAsJsonObject()).thenReturn(jsonObject);

        final JsonEnvelope result = sjpQueryController.getOutstandingFines(query);

        final JsonEnvelope value = jsonEnvelopeCaptor.getValue();
        assertThat(result.payloadAsJsonObject(), is(jsonObject));
        assertThat(profilingView.getFirstName(), is(value.payloadAsJsonObject().getString("firstname")));
        assertThat(profilingView.getLastName(), is(value.payloadAsJsonObject().getString("lastname")));
        if (profilingView.getDateOfBirth() != null)
            assertThat(profilingView.getDateOfBirth().toString(), is(value.payloadAsJsonObject().getString("dob")));

        if (profilingView.getNationalInsuranceNumber() != null)
            assertThat(profilingView.getNationalInsuranceNumber(), is(value.payloadAsJsonObject().getString("ninumber")));
    }

    @Test
    public void shouldReturnEmptyForOutstandingFinesWithUnknownDefendant() {
        final Metadata metadata = metadataOf(
                randomUUID(), "sjp.query.defendant-profile"
        ).build();
        final JsonEnvelope query = envelopeFrom(metadata,
                createObjectBuilder()
                        .add("defendantId", randomUUID().toString())
                        .build());

        when(requester.request(any(JsonEnvelope.class), eq(DefendantProfilingView.class))).thenReturn(
                new DefaultEnvelopeProvider().envelopeFrom(metadata, null)
        );

        final JsonEnvelope result = sjpQueryController.getOutstandingFines(query);

        assertThat(result, jsonEnvelope(
                withMetadataEnvelopedFrom(query).withName("sjp.query.defendant-outstanding-fines"),
                JsonEnvelopePayloadMatcher.payloadIsJson(withJsonPath("$.outstandingFines.length()", equalTo(0))
                )));
    }
}
