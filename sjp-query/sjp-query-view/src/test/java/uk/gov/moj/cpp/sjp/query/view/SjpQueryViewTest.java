package uk.gov.moj.cpp.sjp.query.view;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder.aDefendantDetail;
import static uk.gov.moj.cpp.sjp.query.view.SjpQueryView.FIELD_CASE_ID;
import static uk.gov.moj.cpp.sjp.query.view.SjpQueryView.FIELD_QUERY;
import static uk.gov.moj.cpp.sjp.query.view.SjpQueryView.FIELD_URN;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
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
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseSearchResultsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.CasesPendingDatesToAvoidView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCaseByMaterialIdView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;
import uk.gov.moj.cpp.sjp.query.view.service.DatesToAvoidService;
import uk.gov.moj.cpp.sjp.query.view.service.EmployerService;
import uk.gov.moj.cpp.sjp.query.view.service.FinancialMeansService;
import uk.gov.moj.cpp.sjp.query.view.service.UserAndGroupsService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.JsonObject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SjpQueryViewTest {

    private static final String URN = "urn";
    private static final UUID CASE_ID = UUID.randomUUID();

    @Spy
    private Clock clock = new UtcClock();

    @Spy
    private Enveloper enveloper = EnveloperFactory.createEnveloper();

    @Mock
    private JsonEnvelope envelope, outputEnvelope;

    @Mock
    private Metadata metadata;

    @Mock
    private CaseService caseService;

    @Mock
    private UserAndGroupsService userAndGroupsService;

    @Mock
    private FinancialMeansService financialMeansService;

    @Mock
    private OnlinePleaRepository.FinancialMeansOnlinePleaRepository onlinePleaRepository;

    @Mock
    private DatesToAvoidService datesToAvoidService;

    @Mock
    private EmployerService employerService;

    @Mock
    private JsonObject payloadObject;

    @Mock
    private Function<Object, JsonEnvelope> function;

    @Mock
    private CaseSearchResultsView caseSearchResultsView;


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
    public void shouldFindCaseByUrnPostcode() {

        final String urn = "TFL1234567";
        final String postcode = "AB1 2CD";

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.case-by-urn-postcode"))
                .withPayloadOf(urn, "urn")
                .withPayloadOf(postcode, "postcode")
                .build();

        final CaseDetail caseDetail = CaseDetailBuilder.aCase().addDefendantDetail(
                aDefendantDetail().withPostcode(postcode).withId(UUID.randomUUID()).build())
                .withCompleted(false).withProsecutingAuthority(TFL)
                .withCaseId(UUID.randomUUID()).withUrn(urn).build();

        final CaseView caseView = new CaseView(caseDetail);

        when(caseService.findCaseByUrnPostcode(urn, postcode)).thenReturn(caseView);

        final JsonEnvelope result = sjpQueryView.findCaseByUrnPostcode(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.case-response"),
                payload().isJson(allOf(
                        withJsonPath("$.urn", is(urn)),
                        withJsonPath("$.defendant.personalDetails.address.postcode", is(postcode)),
                        withJsonPath("$.completed", is(false))
                ))
        ));
    }

    @Test
    public void shouldFindCaseSearchResults() {
        setupExpectations();
        final String query = "query";

        when(caseService.searchCases(envelope, query)).thenReturn(caseSearchResultsView);
        when(payloadObject.getString(FIELD_QUERY)).thenReturn(query);

        final JsonEnvelope result = sjpQueryView.findCaseSearchResults(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).searchCases(envelope, query);
        verify(function).apply(caseSearchResultsView);
    }

    @Test
    public void shouldSearchCaseByMaterialId() {
        setupExpectations();
        final UUID query = UUID.fromString("dc1c7baf-5230-4580-877d-b4ee25bc7188");
        final UUID caseId = UUID.randomUUID();
        final SearchCaseByMaterialIdView searchCaseByMaterialIdView = new SearchCaseByMaterialIdView(caseId, null);
        when(payloadObject.getString(FIELD_QUERY)).thenReturn(query.toString());
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
        )
//                .thatMatchesSchema() Issue with remote refs, reported to Techpod: https://github.com/CJSCommonPlatform/microservice_framework/issues/648
        );
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
    @Ignore("Issue with remote refs, reported to Techpod: https://github.com/CJSCommonPlatform/microservice_framework/issues/648")
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
                        withJsonPath("$.address.postcode", is(address.getPostcode()))
                ))
        )
//                .thatMatchesSchema() Issue with remote refs, reported to Techpod: https://github.com/CJSCommonPlatform/microservice_framework/issues/648
        );
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
        ))));
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

    @Test
    public void shouldFindPendingDatesToAvoid() {
        final UUID caseId = randomUUID();
        final JsonEnvelope response = mockAndVerifyPendingDatesToAvoid(caseId, UUID.randomUUID());
        assertThat(response, jsonEnvelope(metadata().withName("sjp.pending-dates-to-avoid"), payload().isJson(allOf(
                withJsonPath("$.cases[0].caseId", equalTo(caseId.toString())),
                withJsonPath("$.count", equalTo(2))
        ))));
    }

    @Test
    public void shouldNotFindPendingDatesToAvoid() {
        final JsonEnvelope response = mockAndVerifyPendingDatesToAvoid();
        assertThat(response, jsonEnvelope(metadata().withName("sjp.pending-dates-to-avoid"), payload().isJson(
                withJsonPath("$.count", equalTo(0))
        )));
    }

    private JsonEnvelope mockAndVerifyPendingDatesToAvoid(UUID... caseIds) {
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.pending-dates-to-avoid"))
                .build();
        final List<PendingDatesToAvoid> pendingDatesToAvoidList = Stream.of(caseIds)
                .map(CaseDetail::new)
                .map(PendingDatesToAvoid::new)
                .collect(toList());

        when(datesToAvoidService.findCasesPendingDatesToAvoid(queryEnvelope)).thenReturn(new CasesPendingDatesToAvoidView(pendingDatesToAvoidList));

        final JsonEnvelope response = sjpQueryView.findPendingDatesToAvoid(queryEnvelope);

        verify(datesToAvoidService).findCasesPendingDatesToAvoid(queryEnvelope);

        return response;
    }

    @Test
    public void shouldFindDefendantsOnlinePlea() {
        final UUID caseId = randomUUID();
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.defendants-online-plea"))
                .withPayloadOf(caseId, "caseId")
                .build();
        final UUID defendantId = UUID.fromString("4a950d66-b95f-459b-b77d-5ed308c3be02");
        final UUID offenceId = UUID.fromString("8a962d66-b95f-69b-b77d-9ed308c3be02");
        final OnlinePlea onlinePlea = stubOnlinePlea(caseId, defendantId, offenceId);

        when(userAndGroupsService.canSeeOnlinePleaFinances(queryEnvelope)).thenReturn(true);
        when(onlinePleaRepository.findBy(caseId)).thenReturn(onlinePlea);

        final JsonEnvelope response = sjpQueryView.findDefendantsOnlinePlea(queryEnvelope);

        verify(onlinePleaRepository).findBy(caseId);
        verify(onlinePleaRepository, never()).findOnlinePleaWithoutFinances(any());

        assertThat(response, jsonEnvelope(metadata().withName("sjp.query.defendants-online-plea"), payload().isJson(allOf(
                withJsonPath("$.defendantId", equalTo(defendantId.toString())),
                withJsonPath("$.pleaDetails.plea", equalTo(PleaType.NOT_GUILTY.name())),
                withJsonPath("$.pleaDetails.comeToCourt", equalTo(true))
        ))));
    }

    @Test
    public void shouldFindDefendantsOnlinePleaWithoutFinancesForProsecutor() {
        final UUID caseId = randomUUID();
        final UUID defendantId = UUID.fromString("4a950d66-b95f-459b-b77d-5ed308c3be02");
        final UUID offenceId = UUID.fromString("8a962d66-b95f-69b-b77d-9ed308c3be02");

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.defendants-online-plea"))
                .withPayloadOf(caseId, "caseId")
                .build();
        final OnlinePlea onlinePlea = stubOnlinePlea(caseId, defendantId, offenceId);

        when(userAndGroupsService.canSeeOnlinePleaFinances(queryEnvelope)).thenReturn(false);
        when(onlinePleaRepository.findOnlinePleaWithoutFinances(caseId)).thenReturn(onlinePlea);

        sjpQueryView.findDefendantsOnlinePlea(queryEnvelope);

        verify(onlinePleaRepository, never()).findBy(any());
        verify(onlinePleaRepository).findOnlinePleaWithoutFinances(caseId);
    }

    private OnlinePlea stubOnlinePlea(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        final OffenceDetail offence = new OffenceDetail();
        offence.setId(offenceId);
        offence.setPlea(PleaType.NOT_GUILTY);
        offence.setPleaMethod(PleaMethod.ONLINE);

        final DefendantDetail defendant = new DefendantDetail(defendantId, null, singleton(offence), null);
        final OnlinePlea onlinePlea = new OnlinePlea(
                new PleaUpdated(caseId, offence.getId(), offence.getPlea(),
                        null, "I was not there, they are lying", offence.getPleaMethod(), clock.now())
        );
        onlinePlea.setDefendantDetail(defendant);

        return onlinePlea;
    }

    private void setupCaseExpectations() {
        when(payloadObject.getString(FIELD_CASE_ID)).thenReturn(CASE_ID.toString());
        setupExpectations();
    }

    private void setupExpectations() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
    }
}
