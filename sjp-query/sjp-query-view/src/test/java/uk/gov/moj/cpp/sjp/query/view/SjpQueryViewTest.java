package uk.gov.moj.cpp.sjp.query.view;

import static com.google.common.collect.Lists.newArrayList;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.LocalDate.now;
import static java.util.Collections.emptyList;
import static java.util.Optional.of;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payload;
import static uk.gov.justice.services.test.utils.core.messaging.JsonEnvelopeBuilder.envelope;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataOf;
import static uk.gov.justice.services.test.utils.core.messaging.MetadataBuilderFactory.metadataWithRandomUUID;
import static uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus.IN_PROGRESS;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder.aDefendantDetail;
import static uk.gov.moj.cpp.sjp.query.view.SjpQueryView.ERROR_INVALID_DATE_RANGE;
import static uk.gov.moj.cpp.sjp.query.view.SjpQueryView.ERROR_INVALID_PAGE_NUMBER;

import org.hamcrest.CoreMatchers;
import uk.gov.justice.core.courts.ProsecutionCase;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.exception.ForbiddenRequestException;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.justice.services.messaging.MetadataBuilder;
import uk.gov.justice.services.test.utils.core.enveloper.EnveloperFactory;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.domain.Address;
import uk.gov.moj.cpp.sjp.domain.Benefits;
import uk.gov.moj.cpp.sjp.domain.DefendantOutstandingFineRequest;
import uk.gov.moj.cpp.sjp.domain.DefendantOutstandingFineRequestsQueryResult;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.Income;
import uk.gov.moj.cpp.sjp.domain.IncomeFrequency;
import uk.gov.moj.cpp.sjp.domain.plea.PleaMethod;
import uk.gov.moj.cpp.sjp.event.PleaUpdated;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.AocpOnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.LegalEntityDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.ReserveCase;
import uk.gov.moj.cpp.sjp.persistence.repository.AocpOnlinePleaRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaDetailRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.sjp.query.view.response.ApplicationView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseSearchResultsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.CasesPendingDatesToAvoidView;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantDetailUpdateRequestView;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantDetailsUpdatesView;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantProfilingView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCaseByMaterialIdView;
import uk.gov.moj.cpp.sjp.query.view.service.AssignmentService;
import uk.gov.moj.cpp.sjp.query.view.service.CaseApplicationService;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;
import uk.gov.moj.cpp.sjp.query.view.service.DatesToAvoidService;
import uk.gov.moj.cpp.sjp.query.view.service.DefendantService;
import uk.gov.moj.cpp.sjp.query.view.service.EmployerService;
import uk.gov.moj.cpp.sjp.query.view.service.FinancialMeansService;
import uk.gov.moj.cpp.sjp.query.view.service.ProsecutionCaseService;
import uk.gov.moj.cpp.sjp.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.query.view.service.UserAndGroupsService;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.DefendantPotentialCaseService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.NoResultException;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.moj.cpp.sjp.query.view.service.defendantcase.DefendantPotentialCaseService;

@SuppressWarnings({"squid:S1607", "squid:S5976"})
@ExtendWith(MockitoExtension.class)
public class SjpQueryViewTest {

    private static final String FIELD_CASE_ID = "caseId";
    private static final String FIELD_URN = "urn";
    private static final String FIELD_CORRELATION_ID = "correlationId";
    private static final String FIELD_QUERY = "q";
    private static final String URN = "urn";
    private static final String FIELD_APP_ID = "applicationId";
    private static final UUID CORRELATION_ID = randomUUID();
    private static final UUID CASE_ID = randomUUID();
    private static final UUID APP_ID = randomUUID();
    private static final String LEGAL_ENTITY_NAME = "Samba Team LTD";
    private static final String FIRST_NAME = "Samba";
    private static final String LAST_NAME = "Salsa";
    private static final LocalDate DATE_OF_BIRTH = now().minusYears(40);
    private static final String REGION = "region";
    private static final String DEFENDANT_ID = randomUUID().toString();
    private static final String COMPANY_DEFENDANT_ID = randomUUID().toString();
    private static final String PROSECUTOR_DVLA = "DVLA";
    private static final String PROSECUTOR_TVL = "TVL";

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
    private CaseApplicationService caseApplicationService;

    @Mock
    private DefendantService defendantService;

    @Mock
    private UserAndGroupsService userAndGroupsService;

    @Mock
    private FinancialMeansService financialMeansService;

    @Mock
    private OnlinePleaRepository.FinancialMeansOnlinePleaRepository onlinePleaRepository;

    @Mock
    private OnlinePleaRepository.LegalEntityDetailsOnlinePleaRepository legalEntityDetailsOnlinePleaRepository;

    @Mock
    private AocpOnlinePleaRepository.PersonDetailsOnlinePleaRepository aocpOnlinePleaRepository;

    @Mock
    private OnlinePleaDetailRepository onlinePleaDetailRepository;

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

    @Mock
    private OffenceRepository offenceRepository;

    @Mock
    private ReferenceDataService referenceDataService;

    @Mock
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Mock
    private ProsecutionCaseService prosecutionCaseService;

    @Mock
    private DefendantPotentialCaseService defendantPotentialCaseService;

    @Mock
    private AssignmentService assignmentService;

    @InjectMocks
    private SjpQueryView sjpQueryView;

    @Test
    public void shouldFindCase() {
        when(payloadObject.getString(FIELD_CASE_ID)).thenReturn(CASE_ID.toString());
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        when(envelope.metadata()).thenReturn(metadataBuilder().withId(randomUUID()).withName("name").build());
        final String prosecutingAuthorityFilterValue = "TFL";
        final CaseView caseView = Mockito.mock(CaseView.class);
        final DefendantDetailUpdateRequestView defendantDetailUpdateRequestView = Mockito.mock(DefendantDetailUpdateRequestView.class);
        when(caseService.findCase(CASE_ID)).thenReturn(caseView);
        when(caseService.findDefendantDetailUpdateRequest(CASE_ID)).thenReturn(defendantDetailUpdateRequestView);
        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(Optional.of(randomUUID().toString()));
        when(caseView.getProsecutingAuthority()).thenReturn(prosecutingAuthorityFilterValue);

        when(prosecutingAuthorityProvider.userHasProsecutingAuthorityAccess(envelope, prosecutingAuthorityFilterValue)).thenReturn(true);

        final JsonEnvelope result = sjpQueryView.findCase(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).findCase(CASE_ID);
        verify(function).apply(caseView);
    }

    @Test
    public void shouldNotFindCaseWhenProsecutionAuthorityHasNoAccess() {
        when(payloadObject.getString(FIELD_CASE_ID)).thenReturn(CASE_ID.toString());
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        when(envelope.metadata()).thenReturn(metadataBuilder().withId(randomUUID()).withName("name").build());
        final String prosecutingAuthorityFilterValue = "TFL";
        final CaseView caseView = Mockito.mock(CaseView.class);
        final DefendantDetailUpdateRequestView defendantDetailUpdateRequestView = Mockito.mock(DefendantDetailUpdateRequestView.class);
        when(caseService.findDefendantDetailUpdateRequest(CASE_ID)).thenReturn(defendantDetailUpdateRequestView);
        when(caseService.findCase(CASE_ID)).thenReturn(caseView);
        when(envelope.metadata()).thenReturn(metadata);
        when(metadata.userId()).thenReturn(Optional.of(randomUUID().toString()));
        when(caseView.getProsecutingAuthority()).thenReturn(prosecutingAuthorityFilterValue);

        when(prosecutingAuthorityProvider.userHasProsecutingAuthorityAccess(envelope, prosecutingAuthorityFilterValue)).thenReturn(false);
        assertThrows(ForbiddenRequestException.class, () -> sjpQueryView.findCase(envelope));
    }

    @Test
    public void shouldFindProsecutionCase() {
        when(payloadObject.getString(FIELD_CASE_ID)).thenReturn(CASE_ID.toString());
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        when(envelope.metadata()).thenReturn(metadataBuilder().withId(randomUUID()).withName("name").build());
        final ProsecutionCase prosecutionCaseView = Mockito.mock(ProsecutionCase.class);
        when(prosecutionCaseService.findProsecutionCase(CASE_ID)).thenReturn(prosecutionCaseView);

        sjpQueryView.findProsecutionCase(envelope);
        verify(prosecutionCaseService).findProsecutionCase(CASE_ID);
    }

    @Test
    public void shouldFindCaseByUrn() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        final CaseView caseView = Mockito.mock(CaseView.class);
        when(payloadObject.getString(FIELD_URN)).thenReturn(URN);
        when(caseService.findCaseByUrn(URN)).thenReturn(caseView);

        final JsonEnvelope result = sjpQueryView.findCaseByUrn(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).findCaseByUrn(URN);
        verify(function).apply(caseView);
    }

    @Test
    public void shouldFindCaseByCorrelationId() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        final CaseView caseView = Mockito.mock(CaseView.class);
        when(payloadObject.getString(FIELD_CORRELATION_ID)).thenReturn(CORRELATION_ID.toString());
        when(caseService.findCaseByCorrelationId(CORRELATION_ID)).thenReturn(caseView);

        final JsonEnvelope result = sjpQueryView.findCaseByCorrelationId(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).findCaseByCorrelationId(CORRELATION_ID);
        verify(function).apply(caseView);
    }

    @Test
    public void shouldFindCaseByApplicationId() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        final CaseView caseView = Mockito.mock(CaseView.class);
        when(payloadObject.getString(FIELD_APP_ID)).thenReturn(APP_ID.toString());
        when(caseService.findCaseByApplicationId(APP_ID)).thenReturn(caseView);

        final JsonEnvelope result = sjpQueryView.findCaseByApplicationId(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseService).findCaseByApplicationId(APP_ID);
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

        OffenceDetail offenceDetail = OffenceDetail.builder().withCompensation(BigDecimal.ONE).withAocpStandardPenalty(new BigDecimal("1.1")).build();
        offenceDetail.setSequenceNumber(1);

        CaseDetail caseDetail = CaseDetailBuilder.aCase().withDefendantDetail(
                        aDefendantDetail().withPostcode(postcode).withId(randomUUID()).withOffences(Arrays.asList(offenceDetail)).build())
                .withCosts(BigDecimal.ONE)
                .withVictimSurcharge(BigDecimal.TEN)
                .withCompleted(false).withProsecutingAuthority("TFL")
                .withCaseId(randomUUID()).withUrn(urn).build();

        caseDetail.setAocpEligible(true);
        caseDetail.setAocpTotalCost(caseDetail.getCosts()
                .add(caseDetail.getAocpVictimSurcharge()
                        .add(offenceDetail.getCompensation()
                                .add(offenceDetail.getAocpStandardPenalty()))));

        JsonObject prosecutorPayload = createObjectBuilder()
                .add("fullName", "Transport for London")
                .add("policeFlag", false)
                .build();

        final CaseView caseView = new CaseView(caseDetail, prosecutorPayload);

        when(caseService.findCaseByUrnPostcode(urn, postcode)).thenReturn(caseView);

        final JsonEnvelope result = sjpQueryView.findCaseByUrnPostcode(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.case-response"),
                payload().isJson(allOf(
                        withJsonPath("$.urn", is(urn)),
                        withJsonPath("$.defendant.personalDetails.address.postcode", is(postcode)),
                        withJsonPath("$.completed", is(false)),
                        withJsonPath("$.aocpEligible", is(true)),
                        withJsonPath("$.aocpTotalCost", is(caseDetail.getAocpTotalCost().doubleValue()))
                ))
        ));
    }

    @Test
    public void shouldFindReservedCase() {
        final String postcode = "AB1 2CD";
        final String urn = "TFL1234567";

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.case"))
                .withPayloadOf(urn, "urn")
                .withPayloadOf(CASE_ID, "caseId")
                .build();

        OffenceDetail offenceDetail = OffenceDetail.builder().withCompensation(BigDecimal.ONE).withAocpStandardPenalty(new BigDecimal("1.1")).build();
        offenceDetail.setSequenceNumber(1);

        CaseDetail caseDetail = CaseDetailBuilder.aCase()
                .withDefendantDetail(
                        aDefendantDetail().withPostcode(postcode).withId(randomUUID()).withOffences(Arrays.asList(offenceDetail)).build())
                .withCosts(BigDecimal.ONE)
                .withVictimSurcharge(BigDecimal.TEN)
                .withCompleted(false).withProsecutingAuthority("TFL")
                .withCaseId(randomUUID())
                .withReserveCase(new ReserveCase(CASE_ID, URN, randomUUID(), ZonedDateTime.parse("2007-12-03T10:15:30Z")))
                .build();


        JsonObject prosecutorPayload = createObjectBuilder()
                .add("fullName", "Transport for London")
                .add("policeFlag", false)
                .build();

        final CaseView caseView = new CaseView(caseDetail, prosecutorPayload);
        when(caseService.findCase(CASE_ID)).thenReturn(caseView);
        when(caseService.getUserName(any(), any())).thenReturn("user name and last name");

        final JsonEnvelope result = sjpQueryView.findCase(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.case-response"),
                payload().isJson(allOf(
                        withJsonPath("$.urn", is(caseDetail.getUrn())),
                        withJsonPath("$.reservedBy", is(caseDetail.getReserveCase().get(0).getReservedBy().toString())),
                        withJsonPath("$.reservedAt", is("2007-12-03T10:15:30.000Z")),
                        withJsonPath("$.reservedByName", is("user name and last name"))
                ))
        ));
    }

    @Test
    public void shouldFindCaseByUrnPostcodeForNotAocpEligible() {

        final String urn = "TFL1234567";
        final String postcode = "AB1 2CD";
        final Double totalCost = new Double("13.1");

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.case-by-urn-postcode"))
                .withPayloadOf(urn, "urn")
                .withPayloadOf(postcode, "postcode")
                .build();

        OffenceDetail offenceDetail = OffenceDetail.builder().withCompensation(BigDecimal.ONE).withAocpStandardPenalty(new BigDecimal("1.1")).build();
        offenceDetail.setSequenceNumber(1);

        CaseDetail caseDetail = CaseDetailBuilder.aCase().withDefendantDetail(
                        aDefendantDetail().withPostcode(postcode).withId(randomUUID()).withOffences(Arrays.asList(offenceDetail)).build())
                .withCosts(BigDecimal.ONE)
                .withVictimSurcharge(BigDecimal.TEN)
                .withCompleted(false).withProsecutingAuthority("TFL")
                .withCaseId(randomUUID()).withUrn(urn).build();

        caseDetail.setAocpEligible(null);

        JsonObject prosecutorPayload = createObjectBuilder()
                .add("fullName", "Transport for London")
                .add("policeFlag", false)
                .build();

        final CaseView caseView = new CaseView(caseDetail, prosecutorPayload);

        when(caseService.findCaseByUrnPostcode(urn, postcode)).thenReturn(caseView);

        final JsonEnvelope result = sjpQueryView.findCaseByUrnPostcode(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.case-response"),
                payload().isJson(allOf(
                        withJsonPath("$.urn", is(urn)),
                        withJsonPath("$.defendant.personalDetails.address.postcode", is(postcode)),
                        withJsonPath("$.completed", is(false)),
                        withJsonPath("$.aocpEligible", is(false))
                ))
        ));
    }

    @Test
    public void shouldFindCaseSearchResults() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
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
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        final UUID query = UUID.fromString("dc1c7baf-5230-4580-877d-b4ee25bc7188");
        final UUID caseId = randomUUID();
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
        when(payloadObject.getString(FIELD_CASE_ID)).thenReturn(CASE_ID.toString());
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
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
        final FinancialMeans financialMeans = new FinancialMeans(defendantId, income, benefit, "EMPLOYED", null, null, null, null);

        when(financialMeansService.getFinancialMeans(defendantId)).thenReturn(of(financialMeans));

        final JsonEnvelope result = sjpQueryView.findFinancialMeans(queryEnvelope);

        assertThat(result, jsonEnvelope(metadata().withName("sjp.query.financial-means"),
                payload().isJson(allOf(
                        withJsonPath("$.defendantId", is(defendantId.toString())),
                        withJsonPath("$.income.amount", is(income.getAmount().doubleValue())),
                        withJsonPath("$.income.frequency", equalTo(income.getFrequency().name())),
                        withJsonPath("$.benefits.claimed", is(benefit.getClaimed())),
                        withJsonPath("$.benefits.type", is(benefit.getType())),
                        withJsonPath("$.employmentStatus", is(financialMeans.getEmploymentStatus()))
                )))
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
    public void shouldReturnEmployer() {

        final UUID defendantId = randomUUID();

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.employer"))
                .withPayloadOf(defendantId, "defendantId")
                .build();

        final Address address = new Address("address 1", "address 2", "address 3", "address 4", "address 5", "AB3 4CD");
        final Employer employer = new Employer(defendantId, "KFC", "abcdef", "02020202020", address);

        when(employerService.getEmployer(defendantId)).thenReturn(of(employer));

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
                        withJsonPath("$.address.address5", is(address.getAddress5())),
                        withJsonPath("$.address.postcode", is(address.getPostcode()))
                ))));
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
    public void shouldGetPendingCases() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        final JsonObject payload = createObjectBuilder().build();
        when(caseService.findPendingCasesToPublish(ExportType.PUBLIC)).thenReturn(payload);

        final JsonEnvelope result = sjpQueryView.getPendingCasesToPublish(envelope);

        verify(function).apply(payload);
        assertThat(result, is(outputEnvelope));
    }

    @Test
    public void getPendingCasesToPublishShouldExportPublicReportByDefault() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);

        sjpQueryView.getPendingCasesToPublish(envelope);

        verify(caseService).findPendingCasesToPublish(ExportType.PUBLIC);
    }

    @Test
    public void getPendingCasesToPublishShouldExportPressReport() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        final JsonObject payload = createObjectBuilder().add("export", "press").build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        sjpQueryView.getPendingCasesToPublish(envelope);

        verify(caseService).findPendingCasesToPublish(ExportType.PRESS);
    }

    @Test
    public void getPendingCasesToPublishShouldExportPressDeltaReport() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        final JsonObject payload = createObjectBuilder().add("export", "press").build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        sjpQueryView.getPendingDeltaCasesToPublish(envelope);

        verify(caseService).findPendingDeltaCasesToPublish(ExportType.PRESS);
    }

    @Test
    public void getPendingCasesToPublishShouldExportPublicDeltaReport() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        final JsonObject payload = createObjectBuilder().add("export", "public").build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        sjpQueryView.getPendingDeltaCasesToPublish(envelope);

        verify(caseService).findPendingDeltaCasesToPublish(ExportType.PUBLIC);
    }

    @Test
    public void getPendingCasesToPublishShouldExportPublicReport() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        final JsonObject payload = createObjectBuilder().add("export", "public").build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        sjpQueryView.getPendingCasesToPublish(envelope);

        verify(caseService).findPendingCasesToPublish(ExportType.PUBLIC);
    }

    @Test
    public void getPendingCasesToPublishExportParamShouldBeCaseInsensitive() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        final JsonObject payload = createObjectBuilder().add("export", "Public").build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        sjpQueryView.getPendingCasesToPublish(envelope);

        verify(caseService).findPendingCasesToPublish(ExportType.PUBLIC);
    }

    @Test
    public void getPendingCasesToPublishShouldUseDefaultIfTypeIsUnknown() {
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        final JsonObject payload = createObjectBuilder().add("export", "UnknownType").build();
        when(envelope.payloadAsJsonObject()).thenReturn(payload);

        sjpQueryView.getPendingCasesToPublish(envelope);

        verify(caseService).findPendingCasesToPublish(ExportType.PUBLIC);
    }

    @Test
    public void shouldFindPendingDatesToAvoid() {
        final UUID caseId = randomUUID();
        final JsonEnvelope response = mockAndVerifyPendingDatesToAvoid(caseId, randomUUID());
        assertThat(response, jsonEnvelope(metadata().withName("sjp.pending-dates-to-avoid"), payload().isJson(allOf(
                withJsonPath("$.cases[0].caseId", equalTo(caseId.toString())),
                withJsonPath("$.cases[0].firstName", equalTo(FIRST_NAME)),
                withJsonPath("$.cases[0].lastName", equalTo(LAST_NAME)),
                withJsonPath("$.cases[0].dateOfBirth", equalTo(DATE_OF_BIRTH.toString())),
                withJsonPath("$.cases[1].legalEntityName", equalTo(LEGAL_ENTITY_NAME)),
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

    @Test
    public void shouldFindDefendantsOnlinePlea() {
        final UUID caseId = randomUUID();
        final UUID defendantId = UUID.fromString("4a950d66-b95f-459b-b77d-5ed308c3be02");
        final String offenceCode = "OffenceCode";
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.defendants-online-plea"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(defendantId, "defendantId")
                .build();

        final UUID offenceId = UUID.fromString("8a962d66-b95f-69b-b77d-9ed308c3be02");
        final OnlinePlea onlinePlea = stubOnlinePlea(caseId, defendantId, offenceId);
        final AocpOnlinePlea aocpOnlinePlea = stubAocpOnlinePlea(caseId, defendantId, offenceId);

        when(userAndGroupsService.canSeeOnlinePleaFinances(queryEnvelope)).thenReturn(true);
        when(onlinePleaRepository.findBy(caseId)).thenReturn(onlinePlea);
        when(legalEntityDetailsOnlinePleaRepository.findBy(caseId)).thenReturn(onlinePlea);
        when(aocpOnlinePleaRepository.findAocpPleaByCaseId(caseId)).thenReturn(aocpOnlinePlea);

        final List<OnlinePleaDetail> onlinePleaDetails = getOnlinePleaDetails(offenceId);
        when(onlinePleaDetailRepository.findByCaseIdAndDefendantIdAndAocpPleaIsNull(caseId, defendantId)).thenReturn(onlinePleaDetails);

        final OffenceDetail offenceDetail = new OffenceDetail();
        offenceDetail.setCode(offenceCode);
        when(offenceRepository.findBy(offenceId)).thenReturn(offenceDetail);

        final JsonObject offenceData = createObjectBuilder()
                .add("title", "Offence title").build();
        when(referenceDataService.getOffenceData(offenceCode)).thenReturn(of(offenceData));

        final JsonEnvelope response = sjpQueryView.findDefendantsOnlinePlea(queryEnvelope);

        verify(onlinePleaRepository).findBy(caseId);
        verify(onlinePleaRepository, never()).findOnlinePleaWithoutFinances(any());

        assertThat(response, jsonEnvelope(metadata().withName("sjp.query.defendants-online-plea"), payload().isJson(allOf(
                withJsonPath("$.pleas[1].defendantId", equalTo(defendantId.toString())),
                withJsonPath("$.pleas[1].pleaDetails.comeToCourt", equalTo(true)),
                withJsonPath("$.pleas[0].aocpAccepted", equalTo(true)),
                withJsonPath("$.pleas[1].onlinePleaDetails[0].plea", equalTo(GUILTY.name())),
                withJsonPath("$.pleas[1].onlinePleaDetails[0].mitigation", equalTo("mitigation")),
                withJsonPath("$.pleas[1].onlinePleaDetails[1].plea", equalTo(NOT_GUILTY.name())),
                withJsonPath("$.pleas[1].onlinePleaDetails[1].notGuiltyBecause", equalTo("Not Guilty Because"))
        ))));
    }

    @Test
    public void shouldFindDefendantsOnlinePleaWithoutFinancesForProsecutor() {
        final UUID caseId = randomUUID();
        final UUID defendantId = UUID.fromString("4a950d66-b95f-459b-b77d-5ed308c3be02");
        final String offenceCode = "OffenceCode";
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.defendants-online-plea"))
                .withPayloadOf(caseId, "caseId")
                .withPayloadOf(defendantId, "defendantId")
                .build();

        final UUID offenceId = UUID.fromString("8a962d66-b95f-69b-b77d-9ed308c3be02");
        final OnlinePlea onlinePlea = stubOnlinePlea(caseId, defendantId, offenceId);
        final AocpOnlinePlea aocpOnlinePlea = stubAocpOnlinePlea(caseId, defendantId, offenceId);

        when(userAndGroupsService.canSeeOnlinePleaFinances(queryEnvelope)).thenReturn(false);
        when(onlinePleaRepository.findOnlinePleaWithoutFinances(caseId)).thenReturn(onlinePlea);

        final List<OnlinePleaDetail> onlinePleaDetails = getOnlinePleaDetails(offenceId);
        when(onlinePleaDetailRepository.findByCaseIdAndDefendantIdAndAocpPleaIsNull(caseId, defendantId)).thenReturn(onlinePleaDetails);
        when(aocpOnlinePleaRepository.findAocpPleaByCaseId(caseId)).thenReturn(aocpOnlinePlea);

        final OffenceDetail offenceDetail = new OffenceDetail();
        offenceDetail.setCode(offenceCode);
        when(offenceRepository.findBy(offenceId)).thenReturn(offenceDetail);

        final JsonObject offenceData = Json.createObjectBuilder()
                .add("title", "Offence title").build();
        when(referenceDataService.getOffenceData(offenceCode)).thenReturn(of(offenceData));

        final JsonEnvelope response = sjpQueryView.findDefendantsOnlinePlea(queryEnvelope);

        verify(onlinePleaRepository, never()).findBy(any());
        verify(onlinePleaRepository).findOnlinePleaWithoutFinances(caseId);

        assertThat(response, jsonEnvelope(metadata().withName("sjp.query.defendants-online-plea"), payload().isJson(allOf(
                withJsonPath("$.pleas[1].defendantId", equalTo(defendantId.toString())),
                withJsonPath("$.pleas[0].aocpAccepted", equalTo(true)),
                withJsonPath("$.pleas[1].pleaDetails.comeToCourt", equalTo(true)),
                withJsonPath("$.pleas[1].onlinePleaDetails[0].plea", equalTo(GUILTY.name())),
                withJsonPath("$.pleas[1].onlinePleaDetails[0].mitigation", equalTo("mitigation")),
                withJsonPath("$.pleas[1].onlinePleaDetails[1].plea", equalTo(NOT_GUILTY.name())),
                withJsonPath("$.pleas[1].onlinePleaDetails[1].notGuiltyBecause", equalTo("Not Guilty Because"))
        ))));
    }

    @Test
    public void shouldFindDetailDetailUpdates() {

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.defendant-details-updates"))
                .build();

        LocalDate dateOfBirth = now().minusYears(30);

        PersonalDetails personalDetails = new PersonalDetails();
        personalDetails.setFirstName("firstName");
        personalDetails.setLastName("lastName");
        personalDetails.setDateOfBirth(dateOfBirth);
        personalDetails.markDateOfBirthUpdated(ZonedDateTime.now());

        String updatedOn = ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        DefendantDetailsUpdatesView.DefendantDetailsUpdate defendantDetailsUpdate = new DefendantDetailsUpdatesView.DefendantDetailsUpdate(
                FIRST_NAME,
                LAST_NAME,
                DEFENDANT_ID,
                CASE_ID.toString(),
                URN,
                dateOfBirth.format(DateTimeFormatter.ISO_LOCAL_DATE),
                true,
                true,
                true,
                updatedOn,
                REGION,
                null);

        DefendantDetailsUpdatesView.DefendantDetailsUpdate defendantDetailsUpdateCompany = new DefendantDetailsUpdatesView.DefendantDetailsUpdate(
                null,
                null,
                COMPANY_DEFENDANT_ID,
                CASE_ID.toString(),
                URN,
                null,
                true,
                false,
                true,
                updatedOn,
                REGION,
                LEGAL_ENTITY_NAME);


        when(defendantService.findDefendantDetailUpdates(queryEnvelope)).thenReturn(new DefendantDetailsUpdatesView(
                2,
                newArrayList(defendantDetailsUpdate, defendantDetailsUpdateCompany)));

        final JsonEnvelope response = sjpQueryView.findDefendantDetailUpdates(queryEnvelope);

        assertThat(response,
                jsonEnvelope(
                        metadata().withName("sjp.query.defendant-details-updates"),
                        payload().isJson(allOf(
                                withJsonPath("$.total", equalTo(2)),
                                withJsonPath("$.defendantDetailsUpdates[0].firstName", equalTo(FIRST_NAME)),
                                withJsonPath("$.defendantDetailsUpdates[0].lastName", equalTo(LAST_NAME)),
                                withJsonPath("$.defendantDetailsUpdates[0].defendantId", equalTo(DEFENDANT_ID)),
                                withJsonPath("$.defendantDetailsUpdates[0].caseUrn", equalTo(URN)),
                                withJsonPath("$.defendantDetailsUpdates[0].caseId", equalTo(CASE_ID.toString())),
                                withJsonPath("$.defendantDetailsUpdates[0].dateOfBirth", equalTo(dateOfBirth.format(DateTimeFormatter.ISO_LOCAL_DATE))),
                                withJsonPath("$.defendantDetailsUpdates[0].nameUpdated", equalTo(true)),
                                withJsonPath("$.defendantDetailsUpdates[0].dateOfBirthUpdated", equalTo(true)),
                                withJsonPath("$.defendantDetailsUpdates[0].addressUpdated", equalTo(true)),
                                withJsonPath("$.defendantDetailsUpdates[0].updatedOn", equalTo(updatedOn)),
                                withJsonPath("$.defendantDetailsUpdates[1].legalEntityName", equalTo(LEGAL_ENTITY_NAME)),
                                withJsonPath("$.defendantDetailsUpdates[1].nameUpdated", equalTo(true)),
                                withJsonPath("$.defendantDetailsUpdates[1].region", equalTo(REGION)),
                                withJsonPath("$.defendantDetailsUpdates[1].defendantId", equalTo(COMPANY_DEFENDANT_ID)),
                                withJsonPath("$.defendantDetailsUpdates[1].caseId", equalTo(CASE_ID.toString()))
                        ))));
    }

    @Test
    public void shouldFindDefendantProfilingView() {
        UUID defendantId = randomUUID();
        Metadata metadata = metadataOf(
                randomUUID(), "sjp.query.defendant-profile"
        ).build();
        final JsonEnvelope query = envelopeFrom(metadata,
                createObjectBuilder()
                        .add("defendantId", defendantId.toString())
                        .build());

        DefendantProfilingView defendantProfilingView = DefendantProfilingView.newBuilder()
                .withFirstName("name")
                .build();
        when(defendantService.getDefendantProfilingView(defendantId)).thenReturn(
                defendantProfilingView
        );

        final JsonEnvelope response = sjpQueryView.getDefendantProfile(query);

        assertThat(response,
                jsonEnvelope(
                        metadata().withName("sjp.query.defendant-profile"),
                        payload().isJson(allOf(
                                withJsonPath("$.firstName", equalTo("name")))
                        )));

    }

    @Test
    public void shouldGetNotGuiltyPleaCases() {
        final String prosecutingAuthority = "TFL";
        ZonedDateTime pleaDate = ZonedDateTime.parse("2018-03-20T18:14:29.894Z");

        final JsonObject casesJson = buildNotGuiltyPleaCases(pleaDate);

        when(caseService.buildNotGuiltyPleaCasesView(prosecutingAuthority, 1, 1)).thenReturn(casesJson);

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.not-guilty-plea-cases"))
                .withPayloadOf(prosecutingAuthority, "prosecutingAuthority")
                .withPayloadOf(1, "pageSize")
                .withPayloadOf(1, "pageNumber")
                .build();

        final JsonEnvelope responseEnvelope = sjpQueryView.getNotGuiltyPleaCases(queryEnvelope);

        assertThat(responseEnvelope.metadata().name(), is("sjp.query.not-guilty-plea-cases"));

        assertThat(responseEnvelope.payloadAsJsonObject().toString(),
                isJson(Matchers.allOf(
                        withJsonPath("results", is(1)),
                        withJsonPath("pageCount", is(1)),
                        withJsonPath("cases[0].id", is(CASE_ID.toString())),
                        withJsonPath("cases[0].urn", is(URN)),
                        withJsonPath("cases[0].firstName", is("Hakan")),
                        withJsonPath("cases[0].lastName", is("Kurtulus")),
                        withJsonPath("cases[0].pleaDate", is(pleaDate.toString())),
                        withJsonPath("cases[0].prosecutingAuthority", is("Transport for London")),
                        withJsonPath("cases[0].caseManagementStatus", is(IN_PROGRESS.name())))));
    }

    @Test
    public void shouldGetCasesWithoutDefendantPostcode() {
        final LocalDate postingDate = LocalDate.parse("2018-03-20");

        final JsonObject casesJson = buildCasesWithoutPostcode(postingDate);
        final int pageSize = 20;
        final int pageNumber = 1;
        final int totalResults = 1;
        final int pageCount = 1;

        when(caseService.buildCasesWithoutDefendantPostcodeView(pageSize, pageNumber)).thenReturn(casesJson);

        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.cases-without-defendant-postcode"))
                .withPayloadOf(pageSize, "pageSize")
                .withPayloadOf(pageNumber, "pageNumber")
                .build();

        final JsonEnvelope responseEnvelope = sjpQueryView.getCasesWithoutDefendantPostcode(queryEnvelope);

        assertThat(responseEnvelope.metadata().name(), is("sjp.query.cases-without-defendant-postcode"));

        assertThat(responseEnvelope.payloadAsJsonObject().toString(),
                isJson(Matchers.allOf(
                        withJsonPath("results", is(totalResults)),
                        withJsonPath("pageCount", is(pageCount)),
                        withJsonPath("cases[0].id", is(CASE_ID.toString())),
                        withJsonPath("cases[0].urn", is(URN)),
                        withJsonPath("cases[0].firstName", is("Hakan")),
                        withJsonPath("cases[0].lastName", is("Kurtulus")),
                        withJsonPath("cases[0].postingDate", is(postingDate.toString())),
                        withJsonPath("cases[0].prosecutingAuthority", is("Transport for London"))
                ))
        );
    }

    @Test
    public void getOutstandingFineRequestsWithNoResults() {

        when(defendantService.getOutstandingFineRequests()).thenThrow(NoResultException.class);

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUID("sjp.query.outstanding-fine-requests"),
                createObjectBuilder()
                        .build());

        final JsonEnvelope result = sjpQueryView.getOutstandingFineRequests(query);

        assertThat(result.metadata().name(), is("sjp.query.outstanding-fine-requests"));
        assertTrue(result.payloadAsJsonObject().isEmpty());
    }

    @Test
    public void getOutstandingFineRequestsWithResults() {

        when(defendantService.getOutstandingFineRequests()).thenReturn(createDefendantRequestProfile());

        final JsonEnvelope query = envelopeFrom(metadataWithRandomUUID("sjp.query.outstanding-fine-requests"),
                createObjectBuilder()
                        .build());

        final JsonEnvelope result = sjpQueryView.getOutstandingFineRequests(query);
        assertThat(result.metadata().name(), is("sjp.query.outstanding-fine-requests"));
        assertTrue(result.payloadAsJsonObject().getJsonArray("defendantDetails").size() == 3);

    }

    @Test
    public void shouldFindApplication() {
        setupAppExpectations();
        final Optional<ApplicationView> appView = Optional.of(Mockito.mock(ApplicationView.class));
        when(caseApplicationService.findApplication(APP_ID)).thenReturn(appView);

        final JsonEnvelope result = sjpQueryView.findApplication(envelope);

        assertEquals(result, outputEnvelope);
        verify(caseApplicationService).findApplication(APP_ID);
        verify(function).apply(appView.get());
    }

    @Test
    public void shouldGetCasesForSOCCheck() {
        final int pageSize = 20;
        final int pageNumber = 1;
        final String courtHouseCode = "B01LY00";
        final String fromDate = "2019-03-23";
        final String toDate = "2021-03-23";
        final int percentage = 50;
        final String sortOrder = "asc";
        final String sortField = "magistrate";
        final String ljaCode = "2577";
        final UUID userId = randomUUID();

        MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder().withUserId(userId.toString()).withId(randomUUID()).withName("sjp.query.cases-for-soc-check");
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataBuilder)
                .withPayloadOf(courtHouseCode, "courtHouseCode")
                .withPayloadOf(fromDate, "fromDate")
                .withPayloadOf(toDate, "toDate")
                .withPayloadOf(percentage, "percentage")
                .withPayloadOf(sortOrder, "sortOrder")
                .withPayloadOf(sortField, "sortField")
                .withPayloadOf(ljaCode, "ljaCode")
                .withPayloadOf(pageSize, "pageSize")
                .withPayloadOf(pageNumber, "pageNumber")
                .build();

        when(caseService.buildCasesForSOCCheckView(userId.toString(), courtHouseCode, ljaCode, LocalDates.from(fromDate),
                LocalDates.from(toDate), percentage, pageSize, pageNumber, sortField, sortOrder, queryEnvelope)).thenReturn(buildCasesForSOCCheck());
        JsonEnvelope responseEnvelope = sjpQueryView.getCasesForSOCCheck(queryEnvelope);
        assertThat(responseEnvelope.metadata().name(), is("sjp.query.cases-for-soc-check"));
        assertThat(responseEnvelope.payloadAsJsonObject().toString(),
                isJson(Matchers.allOf(
                        withJsonPath("totalCount", is(1)),
                        withJsonPath("numberOfPages", is(1)),
                        withJsonPath("pageNumber", is(1)),
                        withJsonPath("pageSize", is(20)),
                        withJsonPath("cases[0].id", is(CASE_ID.toString())),
                        withJsonPath("cases[0].urn", is(URN)),
                        withJsonPath("cases[0].magistrate", is("Test")),
                        withJsonPath("cases[0].legalAdvisor", is("Erica Wilson")),
                        withJsonPath("cases[0].prosecutingAuthority", is("Transport for London"))
                ))
        );
    }

    @Test
    public void shouldGetExceptionForSOCCheckForInvalidPageNumber() {
        final int pageSize = 20;
        final int pageNumber = -1;
        final String courtHouseCode = "B01LY00";
        final String fromDate = "2019-03-23";
        final String toDate = "2021-03-23";
        final int percentage = 50;
        final String sortOrder = "asc";
        final String sortField = "magistrate";
        final String ljaCode = "2577";
        final UUID userId = randomUUID();

        MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder().withUserId(userId.toString()).withId(randomUUID()).withName("sjp.query.cases-for-soc-check");
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataBuilder)
                .withPayloadOf(courtHouseCode, "courtHouseCode")
                .withPayloadOf(fromDate, "fromDate")
                .withPayloadOf(toDate, "toDate")
                .withPayloadOf(percentage, "percentage")
                .withPayloadOf(sortOrder, "sortOrder")
                .withPayloadOf(sortField, "sortField")
                .withPayloadOf(ljaCode, "ljaCode")
                .withPayloadOf(pageSize, "pageSize")
                .withPayloadOf(pageNumber, "pageNumber")
                .build();

        var e = assertThrows(IllegalArgumentException.class, () -> sjpQueryView.getCasesForSOCCheck(queryEnvelope));
        assertThat(e.getMessage(), CoreMatchers.is(String.format(ERROR_INVALID_PAGE_NUMBER, pageNumber, pageSize)));
    }

    @Test
    public void shouldGetExceptionForSOCCheckForInvalidPageSize() {
        final int pageSize = -20;
        final int pageNumber = 1;
        final String courtHouseCode = "B01LY00";
        final String fromDate = "2019-03-23";
        final String toDate = "2021-03-23";
        final int percentage = 50;
        final String sortOrder = "asc";
        final String sortField = "magistrate";
        final String ljaCode = "2577";
        final UUID userId = randomUUID();

        MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder().withUserId(userId.toString()).withId(randomUUID()).withName("sjp.query.cases-for-soc-check");
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataBuilder)
                .withPayloadOf(courtHouseCode, "courtHouseCode")
                .withPayloadOf(fromDate, "fromDate")
                .withPayloadOf(toDate, "toDate")
                .withPayloadOf(percentage, "percentage")
                .withPayloadOf(sortOrder, "sortOrder")
                .withPayloadOf(sortField, "sortField")
                .withPayloadOf(ljaCode, "ljaCode")
                .withPayloadOf(pageSize, "pageSize")
                .withPayloadOf(pageNumber, "pageNumber")
                .build();

        var e = assertThrows(IllegalArgumentException.class, () -> sjpQueryView.getCasesForSOCCheck(queryEnvelope));
        assertThat(e.getMessage(), CoreMatchers.is(String.format(ERROR_INVALID_PAGE_NUMBER, pageNumber, pageSize)));
    }


    @Test
    public void shouldGetExceptionForSOCCheckForInvalidDateRange() {
        final int pageSize = 20;
        final int pageNumber = 1;
        final String courtHouseCode = "B01LY00";
        final String toDate = "2019-03-23";
        final String fromDate = "2021-03-23";
        final int percentage = 50;
        final String sortOrder = "asc";
        final String sortField = "magistrate";
        final String ljaCode = "2577";
        final UUID userId = randomUUID();

        MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder().withUserId(userId.toString()).withId(randomUUID()).withName("sjp.query.cases-for-soc-check");
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataBuilder)
                .withPayloadOf(courtHouseCode, "courtHouseCode")
                .withPayloadOf(fromDate, "fromDate")
                .withPayloadOf(toDate, "toDate")
                .withPayloadOf(percentage, "percentage")
                .withPayloadOf(sortOrder, "sortOrder")
                .withPayloadOf(sortField, "sortField")
                .withPayloadOf(ljaCode, "ljaCode")
                .withPayloadOf(pageSize, "pageSize")
                .withPayloadOf(pageNumber, "pageNumber")
                .build();

        var e = assertThrows(IllegalArgumentException.class, () -> sjpQueryView.getCasesForSOCCheck(queryEnvelope));
        assertThat(e.getMessage(), CoreMatchers.is(String.format(ERROR_INVALID_DATE_RANGE, fromDate, toDate)));

    }

    @Test
    public void shouldGetProsecutingAuthoritiesForLja() {
        final String ljaCode = "2577";
        final MetadataBuilder metadataBuilder = JsonEnvelope.metadataBuilder().withUserId(randomUUID().toString()).withId(randomUUID()).withName("sjp.query.prosecuting-authority-for-lja");
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataBuilder)
                .withPayloadOf(ljaCode, "ljaCode")
                .build();
        when(assignmentService.getProsecutingAuthorityByLja(ljaCode)).thenReturn(Arrays.asList(PROSECUTOR_DVLA, PROSECUTOR_TVL));
        JsonEnvelope responseEnvelope = sjpQueryView.getProsecutingAuthorityForLja(queryEnvelope);
        assertThat(responseEnvelope.metadata().name(), is("sjp.query.prosecuting-authority-for-lja"));
        assertThat(responseEnvelope.payloadAsJsonObject().toString(),
                isJson(Matchers.allOf(
                        withJsonPath("prosecutors", is(notNullValue())),
                        withJsonPath("prosecutors[0]", is(PROSECUTOR_DVLA)),
                        withJsonPath("prosecutors[1]", is(PROSECUTOR_TVL))
                ))
        );
    }

    private JsonObject buildCasesForSOCCheck() {
        return createObjectBuilder()
                .add("totalCount", 1)
                .add("numberOfPages", 1)
                .add("pageNumber", 1)
                .add("pageSize", 20)
                .add("cases", createArrayBuilder().add(createObjectBuilder()
                        .add("id", CASE_ID.toString())
                        .add("urn", URN)
                        .add("lastUpdatedDate", "2022-04-26T09:58:00.913Z")
                        .add("magistrate", "Test")
                        .add("legalAdvisor", "Erica Wilson")
                        .add("prosecutingAuthority", "Transport for London")))
                .build();
    }

    private JsonObject buildNotGuiltyPleaCases(final ZonedDateTime pleaDate) {
        return createObjectBuilder()
                .add("results", 1)
                .add("pageCount", 1)
                .add("cases", createArrayBuilder().add(createObjectBuilder()
                        .add("id", CASE_ID.toString())
                        .add("urn", URN)
                        .add("firstName", "Hakan")
                        .add("lastName", "Kurtulus")
                        .add("pleaDate", pleaDate.toString())
                        .add("prosecutingAuthority", "Transport for London")
                        .add("caseManagementStatus", IN_PROGRESS.name())))
                .build();
    }

    private JsonObject buildCasesWithoutPostcode(final LocalDate postingDate) {
        return createObjectBuilder()
                .add("results", 1)
                .add("pageCount", 1)
                .add("cases", createArrayBuilder().add(createObjectBuilder()
                        .add("id", CASE_ID.toString())
                        .add("urn", URN)
                        .add("firstName", "Hakan")
                        .add("lastName", "Kurtulus")
                        .add("postingDate", postingDate.toString())
                        .add("prosecutingAuthority", "Transport for London")
                ))
                .build();
    }

    private OnlinePlea stubOnlinePlea(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        final OnlinePlea onlinePlea = new OnlinePlea(
                new PleaUpdated(caseId, offenceId, NOT_GUILTY,
                        null, "I was not there, they are lying", PleaMethod.ONLINE, clock.now())
        );
        onlinePlea.setDefendantId(defendantId);

        return onlinePlea;
    }

    private AocpOnlinePlea stubAocpOnlinePlea(final UUID caseId, final UUID defendantId, final UUID offenceId) {
        final AocpOnlinePlea aocpOnlinePlea = new AocpOnlinePlea(
                new PleaUpdated(caseId, offenceId, NOT_GUILTY,
                        null, "I was not there, they are lying", PleaMethod.ONLINE, clock.now())
        );
        aocpOnlinePlea.setDefendantId(defendantId);
        aocpOnlinePlea.setAocpAccepted(true);

        return aocpOnlinePlea;
    }

    private void setupAppExpectations() {
        when(payloadObject.getString(FIELD_APP_ID)).thenReturn(APP_ID.toString());
        when(enveloper.withMetadataFrom(eq(envelope), any())).thenReturn(function);
        when(function.apply(any())).thenReturn(outputEnvelope);
        when(envelope.payloadAsJsonObject()).thenReturn(payloadObject);
        when(envelope.metadata()).thenReturn(metadataBuilder().withId(randomUUID()).withName("name").build());
    }

    private DefendantOutstandingFineRequestsQueryResult createDefendantRequestProfile() {
        return new DefendantOutstandingFineRequestsQueryResult(
                Arrays.asList(
                        DefendantOutstandingFineRequest.newBuilder().withDefendantId(randomUUID()).withDateOfBirth("1980-06-25 00:00:00").withFirstName("Mr").withLastName("Brown").build(),
                        DefendantOutstandingFineRequest.newBuilder().withDefendantId(randomUUID()).withFirstName("Mrs").withLastName("Brown").withNationalInsuranceNumber("AB123456Z").build(),
                        DefendantOutstandingFineRequest.newBuilder().withDefendantId(randomUUID()).withLegalEntityDefendantName("ACME").build()
                )
        );
    }

    private List<OnlinePleaDetail> getOnlinePleaDetails(final UUID offenceId) {
        final List<OnlinePleaDetail> onlinePleaDetails = new ArrayList<>();
        final OnlinePleaDetail onlinePleaDetailGuilty = new OnlinePleaDetail();
        onlinePleaDetailGuilty.setOffenceId(offenceId);
        onlinePleaDetailGuilty.setPlea(GUILTY);
        onlinePleaDetailGuilty.setMitigation("mitigation");
        onlinePleaDetails.add(onlinePleaDetailGuilty);

        final OnlinePleaDetail onlinePleaDetailNotGuilty = new OnlinePleaDetail();
        onlinePleaDetailNotGuilty.setOffenceId(offenceId);
        onlinePleaDetailNotGuilty.setPlea(NOT_GUILTY);
        onlinePleaDetailNotGuilty.setNotGuiltyBecause("Not Guilty Because");
        onlinePleaDetails.add(onlinePleaDetailNotGuilty);

        return onlinePleaDetails;
    }

    private JsonEnvelope mockAndVerifyPendingDatesToAvoid(UUID... caseIds) {
        final JsonEnvelope queryEnvelope = envelope()
                .with(metadataWithRandomUUID("sjp.query.pending-dates-to-avoid"))
                .build();

        final List<PendingDatesToAvoid> pendingDatesToAvoidList = Stream.of(caseIds)
                .map(CaseDetail::new)
                .map(PendingDatesToAvoid::new)
                .collect(toList());
        final CaseDetail caseDetailWithPersonDetails = buildCaseDetail(false);
        final CaseDetail caseDetailWithLegalEntityDetails = buildCaseDetail(true);

        if (pendingDatesToAvoidList.size() > 0) {
            pendingDatesToAvoidList.get(0).setCaseDetail(caseDetailWithPersonDetails);
            pendingDatesToAvoidList.get(1).setCaseDetail(caseDetailWithLegalEntityDetails);
        }
        when(datesToAvoidService.findCasesPendingDatesToAvoid(queryEnvelope)).thenReturn(
                new CasesPendingDatesToAvoidView(pendingDatesToAvoidList, pendingDatesToAvoidList.size()));

        final JsonEnvelope response = sjpQueryView.findPendingDatesToAvoid(queryEnvelope);

        verify(datesToAvoidService).findCasesPendingDatesToAvoid(queryEnvelope);

        return response;
    }

    private CaseDetail buildCaseDetail(boolean isCompany) {
        CaseDetail caseDetail = new CaseDetail();
        DefendantDetail defendantDetail = new DefendantDetail();
        defendantDetail.setAddress(new uk.gov.moj.cpp.sjp.persistence.entity.Address("Test crescent", null, null, null, null, "BH1 1HD"));

        if (isCompany) {
            LegalEntityDetails legalEntityDetails = new LegalEntityDetails();
            legalEntityDetails.setLegalEntityName(LEGAL_ENTITY_NAME);
            defendantDetail.setLegalEntityDetails(legalEntityDetails);
        } else {
            PersonalDetails personalDetails = new PersonalDetails();
            personalDetails.setFirstName(FIRST_NAME);
            personalDetails.setLastName(LAST_NAME);
            personalDetails.setDateOfBirth(DATE_OF_BIRTH);
            defendantDetail.setPersonalDetails(personalDetails);
        }
        caseDetail.setDefendant(defendantDetail);
        return caseDetail;
    }
}
