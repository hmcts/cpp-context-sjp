package uk.gov.moj.cpp.sjp.query.api;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withoutJsonPath;
import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory.createEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.withMetadataEnvelopedFrom;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;

import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher;

import java.util.UUID;

import javax.json.JsonObjectBuilder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AllFinancialMeansQueryApiTest {

    private final UUID defendantId = UUID.randomUUID();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();
    @Mock
    private Requester requester;

    @InjectMocks
    private SjpQueryApi sjpQueryApi;

    public static JsonEnvelopeMatcher queryEnvelope(final JsonEnvelope sourceEnvelope,
                                                    final UUID defendantId, final String name) {
        return jsonEnvelope(withMetadataEnvelopedFrom(sourceEnvelope).withName(name),
                payloadIsJson(withJsonPath("$.defendantId", equalTo(defendantId.toString()))));
    }

    @Test
    public void shouldReturnFinancialDetailWithEmploymentAndEmployerWhenTheEmpStatusIsEmployed() {
        final JsonEnvelope requestEnvelope = createEnvelope("sjp.query.all-financial-means",
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .build());

        final JsonEnvelope financialMeans = getFinancialMeansEnvelope("EMPLOYED");
        final JsonEnvelope  employerEnvelope = getEmployerEnvelope();

        when(requester.request(argThat(queryEnvelope(requestEnvelope, defendantId, "sjp.query.financial-means")))).thenReturn(financialMeans);
        when(requester.request(argThat(queryEnvelope(requestEnvelope, defendantId, "sjp.query.employer")))).thenReturn(employerEnvelope);

        final JsonEnvelope allFinancialMeans = sjpQueryApi.queryAllFinancialMeans(requestEnvelope);
        assertThat(allFinancialMeans, jsonEnvelope(
                metadata().withName("sjp.query.all-financial-means"),
                payloadIsJson(allOf(
                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                        withJsonPath("$.income.frequency", equalTo("MONTHLY")),
                        withJsonPath("$.income.amount", equalTo(100.1)),
                        withJsonPath("$.benefits.claimed", equalTo(true)),
                        withJsonPath("$.benefits.type", equalTo("ree text describing benefits type")),
                        withJsonPath("$.employment.status", equalTo("EMPLOYED")),
                        withJsonPath("$.employer.defendantId", equalTo(defendantId.toString())),
                        withJsonPath("$.employer.name", equalTo("ASDA")),
                        withJsonPath("$.employer.employeeReference", equalTo("Mike"))
                        )
                )));
    }

    @Test
    public void shouldReturnFinancialDetailWithEmploymentWhenTheEmpStatusIsOther() {
        final JsonEnvelope requestEnvelope = createEnvelope("sjp.query.all-financial-means",
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .build());

        final JsonEnvelope financialMeans = getFinancialMeansEnvelope("SomeOther");
        final JsonEnvelope  employerEnvelope = getEmployerEnvelope();

        when(requester.request(argThat(queryEnvelope(requestEnvelope, defendantId, "sjp.query.financial-means")))).thenReturn(financialMeans);

        final JsonEnvelope allFinancialMeans = sjpQueryApi.queryAllFinancialMeans(requestEnvelope);
        verify(requester, never()).request(argThat(queryEnvelope(requestEnvelope, defendantId, "sjp.query.employer")));
        assertThat(allFinancialMeans, jsonEnvelope(
                metadata().withName("sjp.query.all-financial-means"),
                payloadIsJson(allOf(
                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                        withJsonPath("$.income.frequency", equalTo("MONTHLY")),
                        withJsonPath("$.income.amount", equalTo(100.1)),
                        withJsonPath("$.benefits.claimed", equalTo(true)),
                        withJsonPath("$.benefits.type", equalTo("ree text describing benefits type")),
                        withJsonPath("$.employment.status", equalTo("OTHER")),
                        withJsonPath("$.employment.details", equalTo("SomeOther")),
                        withoutJsonPath("$.employer.*"))
                )));
    }

    @Test
    public void shouldReturnFinancialDetailWhenEmploymentStatusIsNull() {
        final JsonEnvelope requestEnvelope = createEnvelope("sjp.query.all-financial-means",
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .build());

        final JsonEnvelope financialMeans = getFinancialMeansEnvelope(null);

        when(requester.request(argThat(queryEnvelope(requestEnvelope, defendantId, "sjp.query.financial-means")))).thenReturn(financialMeans);

        final JsonEnvelope allFinancialMeans = sjpQueryApi.queryAllFinancialMeans(requestEnvelope);
        verify(requester, never()).request(argThat(queryEnvelope(requestEnvelope, defendantId, "sjp.query.employer")));
        assertThat(allFinancialMeans, jsonEnvelope(
                metadata().withName("sjp.query.all-financial-means"),
                payloadIsJson(allOf(
                        withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                        withJsonPath("$.income.frequency", equalTo("MONTHLY")),
                        withJsonPath("$.income.amount", equalTo(100.1)),
                        withJsonPath("$.benefits.claimed", equalTo(true)),
                        withJsonPath("$.benefits.type", equalTo("ree text describing benefits type")),
                        withoutJsonPath("$.employment.*"),
                        withoutJsonPath("$.employer.*"))
                )));
    }


    private JsonEnvelope getFinancialMeansEnvelope(final String employmentStatus) {
        final JsonObjectBuilder financialMeansObjectBuilder = createObjectBuilder()
                .add("defendantId", defendantId.toString())
                .add("income", createObjectBuilder()
                        .add("amount", 100.1)
                        .add("frequency", "MONTHLY"))
                .add("benefits", createObjectBuilder()
                        .add("claimed", true)
                        .add("type", "ree text describing benefits type")
                        .build());
        ofNullable(employmentStatus).ifPresent(empStatus -> financialMeansObjectBuilder.add("employmentStatus", empStatus));

        return createEnvelope("sjp.query.financial-means", financialMeansObjectBuilder.build());
    }


    private JsonEnvelope getEmployerEnvelope() {
        return createEnvelope("sjp.query.employer",
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .add("name", "ASDA")
                        .add("employeeReference", "Mike")
                        .add("address", createObjectBuilder().add("address1", "address line 1"))
                        .add("address", createObjectBuilder().add("postcode", "SW191BN"))
                        .build());
    }

}
