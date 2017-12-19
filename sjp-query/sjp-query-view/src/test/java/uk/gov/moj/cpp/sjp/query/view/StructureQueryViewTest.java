package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonObjectMetadata.metadataWithRandomUUID;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.moj.cpp.sjp.query.view.SjpQueryView.FIELD_CASE_ID;
import static uk.gov.moj.cpp.sjp.query.view.SjpQueryView.FIELD_QUERY;
import static uk.gov.moj.cpp.sjp.query.view.SjpQueryView.FIELD_URN;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.justice.services.test.utils.core.matchers.HandlerClassMatcher;
import uk.gov.justice.services.test.utils.core.matchers.HandlerMethodMatcher;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantsView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCaseByMaterialIdView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCasesView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;
import uk.gov.moj.cpp.sjp.query.view.service.EmployerService;
import uk.gov.moj.cpp.sjp.query.view.service.FinancialMeansService;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import javax.json.JsonObject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StructureQueryViewTest {

    private static final String CASE_ID = "caseId";
    private static final String URN = "urn";

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private JsonEnvelope envelope, outputEnvelope;

    @Mock
    private Metadata metadata;

    @Mock
    private CaseService caseService;

    @Mock
    private FinancialMeansService financialMeansService;

    @Mock
    private EmployerService employerService;

    @Mock
    private JsonObject payloadObject;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @InjectMocks
    private SjpQueryView sjpQueryView;

    @Test
    public void shouldFindCase() {
        setupCaseExpectations();
        final CaseView caseView = Mockito.mock(CaseView.class);
        when(caseService.findCase(CASE_ID)).thenReturn(caseView);

        final JsonEnvelope result = sjpQueryView.findCase(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).findCase(CASE_ID);
        verify(function).apply(caseView);
    }

    @Test
    public void shouldFindCaseByUrn() {
        setupExpectations();
        final CaseView caseView = Mockito.mock(CaseView.class);
        when(payloadObject.getString(FIELD_URN)).thenReturn(URN);
        when(caseService.findCaseByUrn(URN)).thenReturn(caseView);

        final JsonEnvelope result = sjpQueryView.findCaseByUrn(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).findCaseByUrn(URN);
        verify(function).apply(caseView);
    }

    @Test
    public void shouldSearchByPersonId() {
        setupExpectations();
        final String personId = randomUUID().toString();
        when(payloadObject.getString(FIELD_QUERY)).thenReturn(personId);
        final SearchCasesView searchCasesView = new SearchCasesView(personId, emptyList());
        when(caseService.searchCasesByPersonId(personId)).thenReturn(searchCasesView);

        final JsonEnvelope result = sjpQueryView.searchCasesByPersonId(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).searchCasesByPersonId(personId);
        verify(function).apply(searchCasesView);
    }

    @Test
    public void shouldFindCaseSearchResults() {
        setupExpectations();
        final String query = "query";
        final JsonObject jsonObject = createObjectBuilder().build();
        when(payloadObject.getString(FIELD_QUERY)).thenReturn(query);
        when(caseService.searchCases(query)).thenReturn(jsonObject);

        final JsonEnvelope result = sjpQueryView.findCaseSearchResults(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).searchCases(query);
        verify(function).apply(jsonObject);
    }

    @Test
    public void shouldSearchCaseByMaterialId() {
        setupExpectations();
        final String query = "dc1c7baf-5230-4580-877d-b4ee25bc7188";
        final String caseId = UUID.randomUUID().toString();
        final SearchCaseByMaterialIdView searchCaseByMaterialIdView = new SearchCaseByMaterialIdView(caseId, null);
        when(payloadObject.getString(FIELD_QUERY)).thenReturn(query);
        when(caseService.searchCaseByMaterialId(query)).thenReturn(searchCaseByMaterialIdView);

        final JsonEnvelope result = sjpQueryView.searchCaseByMaterialId(envelope);

        assertThat(result, equalTo(outputEnvelope));
        verify(caseService).searchCaseByMaterialId(query);
        verify(function).apply(searchCaseByMaterialIdView);
    }

    @Test
    public void shouldFindCaseDocuments() {
        setupCaseExpectations();
        final CaseDocumentsView caseDocumentsView = new CaseDocumentsView(emptyList());
        when(caseService.findCaseDocuments(CASE_ID)).thenReturn(caseDocumentsView);

        final JsonEnvelope result = sjpQueryView.findCaseDocuments(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).findCaseDocuments(CASE_ID);
        verify(function).apply(caseDocumentsView);
    }

    @Test
    public void shouldFindCaseDefendants() {
        setupCaseExpectations();
        final DefendantsView defendantsView = new DefendantsView(emptyList());
        when(caseService.findCaseDefendants(CASE_ID)).thenReturn(defendantsView);

        final JsonEnvelope result = sjpQueryView.findCaseDefendants(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).findCaseDefendants(CASE_ID);
        verify(function).apply(defendantsView);
    }

    @Test
    public void shouldReturnFinancialMeans() {

        final UUID defendantId = randomUUID();

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.financial-means"))
                .withPayloadOf(defendantId, "defendantId")
                .build();

        final Income income = new Income(IncomeFrequency.MONTHLY, BigDecimal.valueOf(1.1));
        final Benefits benefit = new Benefits(true, "benefitType", null);
        final FinancialMeans financialMeans = new FinancialMeans(defendantId, income, benefit, "EMPLOYED");

        when(financialMeansService.getFinancialMeans(defendantId)).thenReturn(Optional.of(financialMeans));

        final JsonEnvelope result = sjpQueryView.findFinancialMeans(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.financial-means"),
                payload().isJson(allOf(
                        withJsonPath("$.defendantId", is(defendantId.toString())),
                        withJsonPath("$.income.amount", is(income.getAmount().doubleValue())),
                        withJsonPath("$.income.frequency", equalTo(income.getFrequency().name())),
                        withJsonPath("$.benefits.claimed", is(benefit.getClaimed())),
                        withJsonPath("$.benefits.type", is(benefit.getType())),
                        withJsonPath("$.employmentStatus", is(financialMeans.getEmploymentStatus()))
                ))
        ).thatMatchesSchema());
    }

    @Test
    public void shouldReturnEmptyPayloadIfFinancialMeansDoesNotExits() {
        final UUID defendantId = randomUUID();

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.financial-means"))
                .withPayloadOf(defendantId, "defendantId")
                .build();

        when(financialMeansService.getFinancialMeans(defendantId)).thenReturn(Optional.empty());

        final JsonEnvelope result = sjpQueryView.findFinancialMeans(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.financial-means"),
                payload().isJson(hasJsonPath("$.*", empty()))
        ));
    }

    @Test
    public void shouldReturnEmployer() {

        final UUID defendantId = randomUUID();

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.employer"))
                .withPayloadOf(defendantId, "defendantId")
                .build();

        final Address address = new Address("address 1", "address 2", "address 3", "address 4", "AB3 4CD");
        final Employer employer = new Employer(defendantId, "KFC", "abcdef", "02020202020", address);

        when(employerService.getEmployer(defendantId)).thenReturn(Optional.of(employer));

        final JsonEnvelope result = sjpQueryView.findEmployer(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.employer"),
                payload().isJson(allOf(
                        withJsonPath("$.defendantId", is(defendantId.toString())),
                        withJsonPath("$.name", is(employer.getName())),
                        withJsonPath("$.employeeReference", equalTo(employer.getEmployeeReference())),
                        withJsonPath("$.phone", is(employer.getPhone())),
                        withJsonPath("$.address.address1", is(address.getAddress1())),
                        withJsonPath("$.address.address2", is(address.getAddress2())),
                        withJsonPath("$.address.address3", is(address.getAddress3())),
                        withJsonPath("$.address.address4", is(address.getAddress4())),
                        withJsonPath("$.address.postCode", is(address.getPostCode()))
                ))
        ).thatMatchesSchema());
    }

    @Test
    public void shouldReturnEmptyPayloadIfEmployerDoesNotExits() {
        final UUID defendantId = randomUUID();

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.employer"))
                .withPayloadOf(defendantId, "defendantId")
                .build();

        when(employerService.getEmployer(defendantId)).thenReturn(Optional.empty());

        final JsonEnvelope result = sjpQueryView.findEmployer(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.employer"),
                payload().isJson(hasJsonPath("$.*", empty()))
        ));
    }

    @Test
    public void shouldGetAwaitingCases() {
        setupExpectations();
        when(envelope.metadata()).thenReturn(metadata);
        final JsonObject payload = createObjectBuilder().build();
        when(caseService.findAwaitingCases()).thenReturn(payload);

        final JsonEnvelope result = sjpQueryView.getAwaitingCases(envelope);

        verify(function).apply(payload);
        assertThat(result, is(outputEnvelope));
    }

    @Test
    public void shouldGetCasesReferredToCourt() {
        setupExpectations();
        when(envelope.metadata()).thenReturn(metadata);
        final JsonObject payload = createObjectBuilder().build();
        when(caseService.findCasesReferredToCourt()).thenReturn(payload);

        final JsonEnvelope result = sjpQueryView.getCasesReferredToCourt(envelope);

        verify(function).apply(payload);
        assertThat(result, is(outputEnvelope));
    }

    @Test
    public void shouldHandleQueries() {
        assertThat(SjpQueryView.class,
                HandlerClassMatcher.isHandlerClass(Component.QUERY_VIEW)
                        .with(HandlerMethodMatcher.method("findNotReadyCasesGroupedByAge")
                                .thatHandles("sjp.query.not-ready-cases-grouped-by-age")));
    }

    @Test
    public void shouldFindNotReadyCasesGroupedByAge() {
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.not-ready-grouped-by-age"))
                .build();

        final JsonObject jsonObject = createObjectBuilder()
                .add("caseCountsByAgeRanges", createArrayBuilder().add(createObjectBuilder()
                        .add("ageFrom", 0)
                        .add("ageTo", 20).add("casesCount", 5)))
                .build();

        when(caseService.getNotReadyCasesGroupedByAge()).thenReturn(jsonObject);

        final JsonEnvelope responseEnvelope = sjpQueryView.findNotReadyCasesGroupedByAge(queryEnvelope);

        assertThat(responseEnvelope, jsonEnvelope(metadata().withName("sjp.query.not-ready-cases-grouped-by-age"), payload().isJson(allOf(
                withJsonPath("$.caseCountsByAgeRanges", hasSize(1)),
                withJsonPath("$.caseCountsByAgeRanges[?(@.ageFrom == 0 && @.ageTo == 20)].casesCount", contains(5))
        ))).thatMatchesSchema());
    }

    @Test
    public void shouldFindOldestCaseAge() {

        final int oldestCaseAge = 31;

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.oldest-case-age"))
                .build();

        final JsonObject payload = createObjectBuilder()
                .add("oldestCaseAge", oldestCaseAge).build();
        when(caseService.getOldestCaseAge()).thenReturn(payload);

        final JsonEnvelope response = sjpQueryView.findOldestCaseAge(queryEnvelope);

        assertThat(response, jsonEnvelope(metadata().withName("sjp.query.oldest-case-age"),
                payload().isJson(withJsonPath("$.oldestCaseAge", equalTo(oldestCaseAge))
                )).thatMatchesSchema());
    }

    private void setupCaseExpectations() {
        when(payloadObject.getString(FIELD_CASE_ID)).thenReturn(CASE_ID);
        setupExpectations();
    }

    private void setupExpectations() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
    }
}
