package uk.gov.moj.cpp.sjp.query.api;

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.api.decorator.DecisionDecorator;
import uk.gov.moj.cpp.sjp.query.api.decorator.OffenceDecorator;
import uk.gov.moj.cpp.sjp.query.service.OffenceFineLevels;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.time.LocalDate;
import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory.createEnveloper;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUIDAndName;

@RunWith(MockitoJUnitRunner.class)
public class SjpFindCaseQueryApiTest {

    private final UUID caseId = randomUUID();

    private final JsonEnvelope originalQueryEnvelope = envelopeFrom(metadataWithRandomUUID("sjp.query.case"),
            createObjectBuilder().add("caseId", caseId.toString()).build());

    private final UUID applicationId = randomUUID();
    private final UUID parentApplicationId = randomUUID();
    private final String appRef = randomUUID().toString();
    private final String applicationStatus = "Draft";
    private final String applicationType = "STAT_DEC";
    private final String dateReceived = LocalDate.now().toString();
    private final String typeApplicationCode = "MC80528";
    private final UUID typeApplicationId = randomUUID();
    private final String outOfTimeReason = "reason1";

    @Mock
    private Requester requester;

    @Mock
    private OffenceDecorator offenceDecorator;

    @Mock
    private DecisionDecorator decisionDecorator;

    @Spy
    private Enveloper enveloper = createEnveloper();

    @InjectMocks
    private SjpQueryApi sjpQueryApi;

    @Test
    public void shouldFindAndDecorateCase() {
        final JsonObject originalCaseDetails = createObjectBuilder()
                .add("caseId", caseId.toString())
                .build();
        final JsonObject caseDetailsDecoratedWithOffences = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("newProperty", "newProperty")
                .build();
        JsonObject caseDetailsDecoratedWithLegalAdviserName = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("newProperty", "newProperty")
                .add("legalAdviserName", "name")
                .build();

        final JsonEnvelope originalCaseResponse = envelopeFrom(metadataWithRandomUUIDAndName(), originalCaseDetails);

        when(requester.request(originalQueryEnvelope)).thenReturn(originalCaseResponse);
        when(offenceDecorator.decorateAllOffences(eq(originalCaseDetails), eq(originalQueryEnvelope), any(WithdrawalReasons.class), any(OffenceFineLevels.class))).thenReturn(caseDetailsDecoratedWithOffences);
        when(decisionDecorator.decorate(eq(caseDetailsDecoratedWithOffences), eq(originalQueryEnvelope), any(WithdrawalReasons.class))).thenReturn(caseDetailsDecoratedWithLegalAdviserName);

        final JsonEnvelope actualCaseResponse = sjpQueryApi.findCase(originalQueryEnvelope);

        assertThat(actualCaseResponse, jsonEnvelope(withMetadataEnvelopedFrom(originalCaseResponse), payloadIsJson(CoreMatchers.allOf(
                withJsonPath("$.caseId", equalTo(caseId.toString())),
                withJsonPath("$.newProperty", equalTo("newProperty")),
                withJsonPath("$.legalAdviserName", equalTo("name"))))));

        verify(offenceDecorator).decorateAllOffences(eq(originalCaseDetails), eq(originalQueryEnvelope), any(WithdrawalReasons.class), any(OffenceFineLevels.class));
    }

    @Test
    public void shouldReturnEmptyCase() {
        final JsonValue originalCaseDetails = JsonValue.NULL;
        final JsonEnvelope originalCaseResponse = envelopeFrom(metadataWithRandomUUIDAndName(), originalCaseDetails);

        when(requester.request(originalQueryEnvelope)).thenReturn(originalCaseResponse);

        final JsonEnvelope actualCaseResponse = sjpQueryApi.findCase(originalQueryEnvelope);

        assertThat(actualCaseResponse, is(originalCaseResponse));

        verify(offenceDecorator, never()).decorateAllOffences(any(), any(), any(), any());
        verify(decisionDecorator, never()).decorate(any(), any(), any());
    }

    @Test
    public void shouldReturnCaseWithApplication() {
        final JsonObject caseApplication = createObjectBuilder()
                .add("applicationId", applicationId.toString())
                .add("parentApplicationId", parentApplicationId.toString())
                .add("applicationReference", appRef)
                .add("applicationStatus", applicationStatus)
                .add("typeApplicationCode", typeApplicationCode)
                .add("typeApplicationId", typeApplicationId.toString())
                .add("applicationType", applicationType)
                .add("dateReceived", dateReceived)
                .add("outOfTimeReasons", outOfTimeReason)
                .add("outOfTime", false)

                .build();

        final JsonObject originalCaseDetails = createObjectBuilder()
                .add("id", caseId.toString())
                .add("caseApplication", caseApplication)
                .build();
        JsonObject caseDetailsDecoratedWithCaseApplication = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("caseApplication", caseApplication)
                .build();

        final JsonObject caseDetailsDecoratedWithOffences = createObjectBuilder()
                .add("caseId", caseId.toString())
                .add("newProperty", "newProperty")
                .build();

        final JsonEnvelope originalCaseResponse = envelopeFrom(metadataWithRandomUUIDAndName(), originalCaseDetails);

        when(requester.request(originalQueryEnvelope)).thenReturn(originalCaseResponse);
        when(requester.request(originalQueryEnvelope)).thenReturn(originalCaseResponse);
        when(offenceDecorator.decorateAllOffences(eq(originalCaseDetails), eq(originalQueryEnvelope), any(WithdrawalReasons.class), any(OffenceFineLevels.class))).thenReturn(caseDetailsDecoratedWithOffences);
        when(decisionDecorator.decorate(eq(caseDetailsDecoratedWithOffences), eq(originalQueryEnvelope), any(WithdrawalReasons.class))).thenReturn(caseDetailsDecoratedWithCaseApplication);

        final JsonEnvelope actualCaseResponse = sjpQueryApi.findCase(originalQueryEnvelope);

        assertThat(actualCaseResponse, jsonEnvelope(withMetadataEnvelopedFrom(originalCaseResponse), payloadIsJson(CoreMatchers.allOf(
                withJsonPath("$.caseId", equalTo(caseId.toString())),
                withJsonPath("$.caseApplication.applicationId", equalTo(applicationId.toString())),
                withJsonPath("$.caseApplication.applicationStatus", equalTo(applicationStatus)),
                withJsonPath("$.caseApplication.typeApplicationCode", equalTo(typeApplicationCode)),
                withJsonPath("$.caseApplication.applicationType", equalTo(applicationType)),
                withJsonPath("$.caseApplication.dateReceived", equalTo(dateReceived)),
                withJsonPath("$.caseApplication.outOfTime", is(false))))));
    }

}
