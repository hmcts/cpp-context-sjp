package uk.gov.moj.cpp.sjp.query.view.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.UUID.randomUUID;
import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.CPS;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.DVLA;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TVL;
import static uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder.aCase;
import static uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder.aDefendantDetail;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityAccess;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDocumentBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSummary;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseCountByAgeView;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseReferredToCourtRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.NotReadyCaseRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseSearchResultsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.CasesMissingSjpnWithDetailsView;
import uk.gov.moj.cpp.sjp.query.view.response.ResultOrdersView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCaseByMaterialIdView;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CaseServiceTest {

    private Clock clock = new StoppedClock(new UtcClock().now());

    private static final UUID CASE_ID = randomUUID();
    private static final String URN = "TFL1234";
    private static final String PROSECUTING_AUTHORITY = "prosecutingAuthority";
    private static final Boolean COMPLETED = Boolean.TRUE;
    private static final String ENTERPRISE_ID = "2K2SLYFC743H";

    private static final LocalDate POSTING_DATE = LocalDate.now(UTC);
    private static final String FIRST_NAME = "Adam";
    private static final String LAST_NAME = "Zuma";
    private static final String POSTCODE = "AB1 2CD";
    private final LocalDate DATE_OF_BIRTH = LocalDate.now(UTC).minusYears(30);

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private NotReadyCaseRepository notReadyCaseRepository;

    @Mock
    private CaseDocumentRepository caseDocumentRepository;

    @Mock
    private CaseSearchResultRepository caseSearchResultRepository;

    @Mock
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Mock
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    @Mock
    private JsonEnvelope envelope;

    @Mock
    private CaseReferredToCourtRepository caseReferredToCourtRepository;

    @InjectMocks
    private CaseService service;

    @Test
    public void shouldFindCaseViewWithDocumentsWherePostalPlea() {
        assertExpectationsForFindCaseViewWithDocuments(false);
    }

    @Test
    public void shouldFindCaseViewWithDocumentsWhereOnlinePlea() {
        assertExpectationsForFindCaseViewWithDocuments(true);
    }

    private void assertExpectationsForFindCaseViewWithDocuments(final boolean onlinePleaReceived) {
        final CaseDetail caseDetail = createCaseDetailWithDocumentTypes(onlinePleaReceived, "FINANCIAL_MEANS", "OTHER", "Travelcard");

        given(caseRepository.findBy(CASE_ID)).willReturn(caseDetail);
        final CaseView caseView = service.findCase(CASE_ID);
        assertThat(caseView, notNullValue());
        assertThat(caseView.getId(), is(CASE_ID.toString()));
        assertThat(caseView.getUrn(), is(URN));
        assertThat(caseView.getCaseDocuments(), hasSize(3));
        assertThat(caseView.isOnlinePleaReceived(), is(onlinePleaReceived));
    }

    @Test
    public void shouldFindCaseViewWithFilteringOfOtherAndFinancialMeansDocuments() {
        final CaseDetail caseDetail = createCaseDetailWithDocumentTypes("FINANCIAL_MEANS", "OTHER", "OTHER-Travelcard");

        given(caseRepository.findBy(CASE_ID)).willReturn(caseDetail);

        final CaseView caseView = service.findCaseAndFilterOtherAndFinancialMeansDocuments(CASE_ID.toString());
        assertThat(caseView.getCaseDocuments().size(), is(0));
    }

    @Test
    public void shouldFindCaseViewWithFilteringWhichLeavesRequiredDocuments() {
        final CaseDetail caseDetail = createCaseDetailWithDocumentTypes("PLEA", "CITN", "SJPN");

        given(caseRepository.findBy(CASE_ID)).willReturn(caseDetail);

        final CaseView caseView = service.findCaseAndFilterOtherAndFinancialMeansDocuments(CASE_ID.toString());
        assertThat(caseView.getCaseDocuments().size(), is(3));
    }

    @Test
    public void shouldFindCaseViewWithFilteringOfUnwantedDocuments() {
        final CaseDetail caseDetail = createCaseDetailWithDocumentTypes("PLEA", "OTHER", "FINANCIAL_MEANS");

        given(caseRepository.findBy(CASE_ID)).willReturn(caseDetail);

        final CaseView caseView = service.findCaseAndFilterOtherAndFinancialMeansDocuments(CASE_ID.toString());
        assertThat(caseView.getCaseDocuments().size(), is(1));
    }

    @Test
    public void shouldFindCaseByUrn() {
        final CaseDetail caseDetail = createCaseDetail(true);

        given(caseRepository.findByUrn(URN)).willReturn(caseDetail);
        final CaseView caseView = service.findCaseByUrn(URN);

        assertThat(caseView, notNullValue());
        assertThat(caseView.getId(), is(CASE_ID.toString()));
        assertThat(caseView.getUrn(), is(URN));
        assertThat(caseView.getDateTimeCreated(), is(clock.now()));
        assertThat(caseView.getCosts(), nullValue());
        assertThat(caseView.isOnlinePleaReceived(), is(true));
    }

    @Test
    public void shouldFindCaseByUrnAndContainsReopenedDateAndLibraCaseNumber() {
        final CaseDetail caseDetail = createCaseDetail(true);
        final LocalDate reopenedDate = LocalDate.now();
        final String reason = "REASON";
        caseDetail.setReopenedDate(reopenedDate);
        caseDetail.setLibraCaseNumber("LIBRA12345");
        caseDetail.setReopenedInLibraReason(reason);

        given(caseRepository.findByUrn(URN)).willReturn(caseDetail);

        final CaseView caseView = service.findCaseByUrn(URN);

        assertThat(caseView, notNullValue());
        assertThat(caseView.getId(), is(CASE_ID.toString()));
        assertThat(caseView.getUrn(), is(URN));
        assertThat(caseView.getLibraCaseNumber(), is("LIBRA12345"));
        assertThat(caseView.getReopenedInLibraReason(), is(reason));
        assertThat(caseView.getReopenedDate(), is(reopenedDate));
        assertThat(caseView.getDateTimeCreated(), is(clock.now()));
        assertThat(caseView.getCosts(), nullValue());
        assertThat(caseView.isOnlinePleaReceived(), is(true));
    }

    @Test
    public void shouldHandleWhenNoCaseFoundForUrn() {
        given(caseRepository.findByUrn(URN)).willThrow(new NoResultException("boom"));
        assertThat(service.findCaseByUrn(URN), nullValue());
    }

    @Test
    public void shouldFindByUrnPostcode() {
        CaseDetail caseDetail = createCaseDetail();

        given(caseRepository.findByUrnPostcode(URN, POSTCODE)).willReturn(caseDetail);
        final CaseView caseView = service.findCaseByUrnPostcode(URN, POSTCODE);

        assertThat(caseView.getId(), is(CASE_ID.toString()));
    }

    @Test
    public void shouldReturnNullWhenNoCaseFoundForUrnAndPostcode() {
        given(caseRepository.findByUrnPostcode(URN, POSTCODE)).willReturn(null);
        assertThat(service.findCaseByUrnPostcode(URN, POSTCODE), nullValue());
    }

    @Test
    public void shouldReturnNullWhenMultipleCaseFoundForUrnAndPostcode() {
        given(caseRepository.findByUrnPostcode(URN, POSTCODE)).willThrow(new NonUniqueResultException());
        assertThat(service.findCaseByUrnPostcode(URN, POSTCODE), nullValue());
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenTVL() {
        final UUID materialId = UUID.randomUUID();
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(CASE_ID);
        caseDetail.setProsecutingAuthority(TVL);

        when(caseRepository.findByMaterialId(materialId)).thenReturn(caseDetail);

        final SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId);

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(CASE_ID));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(),
                is(ProsecutingAuthority.TVL));
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenDVLA() {
        final UUID materialId = UUID.randomUUID();
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(CASE_ID);
        caseDetail.setProsecutingAuthority(DVLA);

        when(caseRepository.findByMaterialId(materialId)).thenReturn(caseDetail);

        final SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId);

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(CASE_ID));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(),
                is(ProsecutingAuthority.DVLA));
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenTFL() {
        final UUID materialId = UUID.randomUUID();
        final CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(CASE_ID);
        caseDetail.setProsecutingAuthority(TFL);

        when(caseRepository.findByMaterialId(materialId)).thenReturn(caseDetail);

        final SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId);

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(CASE_ID));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(),
                is(ProsecutingAuthority.TFL));
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenCaseIdAndProsecutingAuthorityAreNull() {
        final UUID materialId = UUID.randomUUID();

        when(caseRepository.findByMaterialId(materialId)).thenReturn(null);

        final SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId);

        assertThat(searchCaseByMaterialIdView.getCaseId(), nullValue());
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(), nullValue());
    }


    @Test
    public void shouldReturnNullIfCaseNotFoundSearchCaseByMaterialId() {
        final UUID materialId = UUID.randomUUID();

        when(caseRepository.findByMaterialId(materialId)).thenThrow(new NoResultException());

        SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId);

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(nullValue()));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(), is(nullValue()));
    }


    @Test
    public void shouldFindCaseDocuments() {
        final List<CaseDocument> caseDocumentList = new ArrayList<>();
        final UUID documentId = randomUUID();
        final CaseDocument caseDocument = new CaseDocument(documentId, randomUUID(), "SJPN", clock.now(), CASE_ID, 2);
        caseDocumentList.add(caseDocument);
        when(caseRepository.findCaseDocuments(CASE_ID)).thenReturn(caseDocumentList);

        final CaseDocumentsView caseDocumentsView = service.findCaseDocuments(CASE_ID);
        assertThat(caseDocumentsView, notNullValue());
        final CaseDocumentView firstCaseDocument = caseDocumentsView.getCaseDocuments().get(0);
        assertThat(firstCaseDocument.getId().toString(), is(documentId.toString()));
        assertThat(firstCaseDocument.getDocumentNumber(), is(2));
    }

    @Test
    public void shouldFindCaseDocument() {
        final CaseDocument caseDocument = new CaseDocument(randomUUID(), randomUUID(), "SJPN", clock.now(), CASE_ID, 2);

        when(caseRepository.findCaseDocuments(CASE_ID)).thenReturn(Arrays.asList(caseDocument));

        final Optional<CaseDocumentView> caseDocumentView = service.findCaseDocument(CASE_ID, caseDocument.getId());

        assertThat(caseDocumentView.isPresent(), is(true));
        assertThat(caseDocumentView.get().getId(), is(caseDocument.getId()));
        assertThat(caseDocumentView.get().getMaterialId(), is(caseDocument.getMaterialId()));
        assertThat(caseDocumentView.get().getDocumentType(), is(caseDocument.getDocumentType()));
        assertThat(caseDocumentView.get().getDocumentNumber(), is(caseDocument.getDocumentNumber()));
    }

    @Test
    public void shouldReturnEmptyCaseDocumentIfDocumentDoesNotExist() {
        when(caseRepository.findCaseDocuments(CASE_ID)).thenReturn(emptyList());

        final Optional<CaseDocumentView> caseDocumentView = service.findCaseDocument(CASE_ID, randomUUID());

        assertThat(caseDocumentView.isPresent(), is(false));
    }

    @Test
    public void shouldFindCaseDocumentsViewWithFilteringOfOtherAndFinancialMeansDocuments() {
        final List<CaseDocument> caseDocuments = createCaseDocuments("FINANCIAL_MEANS", "OTHER", "OTHER-Travelcard");

        given(caseRepository.findCaseDocuments(CASE_ID)).willReturn(caseDocuments);

        final CaseDocumentsView caseDocumentsView = service.findCaseDocumentsFilterOtherAndFinancialMeans(CASE_ID);
        assertThat(caseDocumentsView.getCaseDocuments().size(), is(0));
    }

    @Test
    public void shouldFindCaseDocumentsViewWithFilteringWhichLeavesRequiredDocuments() {
        final List<CaseDocument> caseDocuments = createCaseDocuments("PLEA", "CITN", "SJPN");

        given(caseRepository.findCaseDocuments(CASE_ID)).willReturn(caseDocuments);

        final CaseDocumentsView caseDocumentsView = service.findCaseDocumentsFilterOtherAndFinancialMeans(CASE_ID);
        assertThat(caseDocumentsView.getCaseDocuments().size(), is(3));
    }

    @Test
    public void shouldFindCaseDocumentsViewWithFilteringOfUnwantedDocuments() {
        final List<CaseDocument> caseDocuments = createCaseDocuments("PLEA", "OTHER", "FINANCIAL_MEANS");

        given(caseRepository.findCaseDocuments(CASE_ID)).willReturn(caseDocuments);

        final CaseDocumentsView caseDocumentsView = service.findCaseDocumentsFilterOtherAndFinancialMeans(CASE_ID);
        assertThat(caseDocumentsView.getCaseDocuments().size(), is(1));
    }

    @Test
    public void shouldFindAwaitingCases() {

        final CaseDetail caseDetail =
                aCase().addDefendantDetail(aDefendantDetail().build()).build();
        when(caseRepository.findAwaitingSjpCases(600)).thenReturn(singletonList(caseDetail));

        final JsonObject awaitingCases = service.findAwaitingCases();

        final JsonObject awaitingCase1 = awaitingCases.getJsonArray("awaitingCases")
                .getValuesAs(JsonObject.class).get(0);
        final DefendantDetail defendantDetail = caseDetail.getDefendant();
        final OffenceDetail offenceDetail = defendantDetail.getOffences().iterator().next();
        assertThat(awaitingCase1.getString("offenceCode"), is(offenceDetail.getCode()));

    }

    @Test
    public void shouldFindResultOrders() {
        //given
        final LocalDate FROM_DATE = LocalDates.from("2017-01-01");
        final LocalDate TO_DATE = LocalDates.from("2017-01-10");

        final DefendantDetail defendantDetail = new DefendantDetail(UUID.randomUUID(), new PersonalDetails(), null, 0);
        final CaseDetail caseDetail = new CaseDetail(UUID.randomUUID(), "TFL1234", "2K2SLYFC743H", null, null, null, null, null, defendantDetail, null, null);

        final CaseDocument caseDocument = new CaseDocument(UUID.randomUUID(),
                UUID.randomUUID(), CaseDocument.RESULT_ORDER_DOCUMENT_TYPE,
                clock.now(), caseDetail.getId(), null);

        final ZonedDateTime FROM_DATE_TIME = FROM_DATE.atStartOfDay(UTC);
        final ZonedDateTime TO_DATE_TIME = TO_DATE.atStartOfDay(UTC);
        when(caseDocumentRepository.findCaseDocumentsOrderedByAddedByDescending(FROM_DATE_TIME,
                TO_DATE_TIME, CaseDocument.RESULT_ORDER_DOCUMENT_TYPE)).thenReturn(singletonList(caseDocument));
        when(caseRepository.findBy(caseDetail.getId())).thenReturn(caseDetail);
        //when
        final ResultOrdersView resultOrdersView = service.findResultOrders(FROM_DATE, TO_DATE);
        //then
        assertEquals(caseDocument.getCaseId(),
                resultOrdersView.getResultOrders().get(0).getCaseId());
        assertEquals(caseDetail.getUrn(),
                resultOrdersView.getResultOrders().get(0).getUrn());
        assertEquals(caseDocument.getMaterialId(),
                resultOrdersView.getResultOrders().get(0).getOrder().getMaterialId());
        assertEquals(caseDocument.getAddedAt(),
                resultOrdersView.getResultOrders().get(0).getOrder().getAddedAt());
    }

    @Test
    public void shouldNotFindResultOrders() {
        //given
        final LocalDate FROM_DATE = LocalDates.from("2017-01-01");
        final LocalDate TO_DATE = LocalDates.from("2017-01-10");

        final DefendantDetail defendantDetail = new DefendantDetail(UUID.randomUUID(), new PersonalDetails(), null, 0);
        final CaseDetail caseDetail = new CaseDetail(UUID.randomUUID(), "TFL1234", "2K2SLYFC743H", null, null, null, null, null, defendantDetail, null, null);
        final CaseDocument caseDocument = new CaseDocument(UUID.randomUUID(),
                UUID.randomUUID(), CaseDocument.RESULT_ORDER_DOCUMENT_TYPE,
                clock.now(), caseDetail.getId(), null);

        final ZonedDateTime FROM_DATE_TIME = FROM_DATE.atStartOfDay(UTC);
        final ZonedDateTime TO_DATE_TIME = TO_DATE.atStartOfDay(UTC);
        when(caseDocumentRepository.findCaseDocumentsOrderedByAddedByDescending(FROM_DATE_TIME,
                TO_DATE_TIME, CaseDocument.RESULT_ORDER_DOCUMENT_TYPE)).thenReturn(
                singletonList(caseDocument));
        when(caseRepository.findBy(caseDetail.getId())).thenReturn(null);
        //when
        final ResultOrdersView resultOrdersView = service.findResultOrders(FROM_DATE, TO_DATE);
        //then
        assertEquals(resultOrdersView.getResultOrders().size(), 0);
    }

    @Test
    public void shouldGroupNotReadyCasesByAgeRange() {

        final List<CaseCountByAgeView> casesCountsInAgeRanges = new ArrayList<>();
        casesCountsInAgeRanges.add(new CaseCountByAgeView(-1, 1));
        casesCountsInAgeRanges.add(new CaseCountByAgeView(0, 1));
        casesCountsInAgeRanges.add(new CaseCountByAgeView(1, 1));
        casesCountsInAgeRanges.add(new CaseCountByAgeView(20, 2));
        casesCountsInAgeRanges.add(new CaseCountByAgeView(21, 3));
        casesCountsInAgeRanges.add(new CaseCountByAgeView(22, 4));
        casesCountsInAgeRanges.add(new CaseCountByAgeView(27, 5));

        when(notReadyCaseRepository.getCountOfCasesByAge()).thenReturn(casesCountsInAgeRanges);

        final JsonObject notReadyCasesGroupedByAge = service.getNotReadyCasesGroupedByAge();
        assertThat(notReadyCasesGroupedByAge.toString(), isJson(allOf(
                withJsonPath("$.caseCountsByAgeRanges", hasSize(2)),
                withJsonPath("$.caseCountsByAgeRanges[?(@.ageTo == 20)].casesCount", contains(5)),
                withJsonPath("$.caseCountsByAgeRanges[?(@.ageFrom == 21 && @.ageTo == 27)].casesCount", contains(12))
        )));
    }

    @Test
    public void shouldGetOldestCaseAge() {

        final int age = 7;
        when(caseRepository.findOldestUncompletedPostingDate()).thenReturn(LocalDate.now().minusDays(age));

        final JsonObject response = service.getOldestCaseAge();

        assertThat(response.getInt("oldestCaseAge"), equalTo(age));
    }

    @Test
    public void shouldSearchCasesByUrn() {
        final String query = URN;
        final ProsecutingAuthorityAccess prosecutingAuthorityAccess = mock(ProsecutingAuthorityAccess.class);
        final String prosecutingAuthorityAccessFilterValue = "SOME_FILTER";

        when(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope))
                .thenReturn(prosecutingAuthorityAccess);
        when(prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityAccess))
                .thenReturn(prosecutingAuthorityAccessFilterValue);
        when(caseSearchResultRepository.findByUrn(prosecutingAuthorityAccessFilterValue, query))
                .thenReturn(singletonList(createCaseSearchResult()));

        final CaseSearchResultsView cases = service.searchCases(envelope, query);

        assertThat(cases.getResults().get(0).getUrn(), equalTo(URN));

        verify(prosecutingAuthorityProvider).getCurrentUsersProsecutingAuthorityAccess(envelope);
    }

    @Test
    public void shouldSearchCasesByLastName() {
        final String query = LAST_NAME;
        final ProsecutingAuthorityAccess prosecutingAuthorityAccess = mock(ProsecutingAuthorityAccess.class);
        final String prosecutingAuthorityAccessFilterValue = "SOME_FILTER";

        when(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope))
                .thenReturn(prosecutingAuthorityAccess);
        when(prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityAccess))
                .thenReturn(prosecutingAuthorityAccessFilterValue);

        when(caseSearchResultRepository.findByUrn(prosecutingAuthorityAccessFilterValue, query))
                .thenReturn(emptyList());
        when(caseSearchResultRepository.findByLastName(prosecutingAuthorityAccessFilterValue, query))
                .thenReturn(singletonList(createCaseSearchResult()));

        final CaseSearchResultsView cases = service.searchCases(envelope, query);

        verify(prosecutingAuthorityProvider).getCurrentUsersProsecutingAuthorityAccess(envelope);

        final CaseSearchResultsView.CaseSearchResultView result = cases.getResults().get(0);
        assertThat(result.getCaseId(), equalTo(CASE_ID));
        assertThat(result.getUrn(), equalTo(URN));
        assertThat(result.getEnterpriseId(), equalTo(ENTERPRISE_ID));
        assertThat(result.getProsecutingAuthority(), equalTo(PROSECUTING_AUTHORITY));
        assertThat(result.getPostingDate(), equalTo(POSTING_DATE));
        assertThat(result.getDefendant().getFirstName(), equalTo(FIRST_NAME));
        assertThat(result.getDefendant().getLastName(), equalTo(LAST_NAME));
        assertThat(result.getDefendant().getDateOfBirth(), equalTo(DATE_OF_BIRTH));
    }

    @Test
    public void findCasesReferredToCourtTest() {
        // given
        when(caseReferredToCourtRepository.findUnactionedCases()).thenReturn(Lists.newArrayList());

        // when
        final JsonObject result = service.findCasesReferredToCourt();

        // then
        assertThat(result.getJsonArray("cases"), hasSize(0));
    }

    @Test
    public void findCasesMissingSjpnWithDetailsTest() {
        // given
        when(caseRepository.findCasesMissingSjpnWithDetails()).thenReturn(Lists.newArrayList());

        // when
        final CasesMissingSjpnWithDetailsView result = service.findCasesMissingSjpnWithDetails(Optional.empty());

        // then
        assertThat(result.getCaseMissingSjpnWithDetailsView(), hasSize(0));

    }

    @Test
    public void shouldReturnCaseDetailsWhenCaseExists() {
        final CaseDetail caseDetail = createCaseDetail();
        when(caseRepository.findBy(CASE_ID)).thenReturn(caseDetail);

        assertThat(service.getCase(CASE_ID).get(), equalTo(caseDetail));
    }

    @Test
    public void shouldReturnEmptyCaseWhenCaseDoesNotExist() {
        when(caseRepository.findBy(CASE_ID)).thenReturn(null);

        assertThat(service.getCase(CASE_ID).isPresent(), is(false));
    }

    private CaseDetail createCaseDetail() {
        return createCaseDetail(false);
    }

    private CaseDetail createCaseDetail(final boolean onlinePleaReceived) {
        final CaseDetail caseDetail = new CaseDetail(CASE_ID, URN, ENTERPRISE_ID, CPS,
                null, COMPLETED, null, clock.now(), new DefendantDetail(), null, null);
        caseDetail.setOnlinePleaReceived(onlinePleaReceived);
        return caseDetail;
    }

    private CaseDetail createCaseDetailWithDocumentTypes(final String... documentTypes) {
        return createCaseDetailWithDocumentTypes(false, documentTypes);
    }

    private CaseDetail createCaseDetailWithDocumentTypes(final boolean onlinePlea, final String... documentTypes) {
        final CaseDetail caseDetail = createCaseDetail(onlinePlea);
        createCaseDocuments(documentTypes).forEach(caseDetail::addCaseDocuments);
        return caseDetail;
    }

    private List<CaseDocument> createCaseDocuments(final String... documentTypes) {
        return Arrays.stream(documentTypes)
                .map(this::createCaseDocument)
                .collect(Collectors.toList());
    }

    private CaseDocument createCaseDocument(String documentType) {
        return CaseDocumentBuilder.aCaseDocument().withDocumentType(documentType).build();
    }

    private CaseSearchResult createCaseSearchResult() {
        final CaseSearchResult caseSearchResult = new CaseSearchResult();
        final CaseSummary caseSummary = new CaseSummary();
        caseSummary.setId(CASE_ID);
        caseSummary.setUrn(URN);
        caseSummary.setEnterpriseId(ENTERPRISE_ID);
        caseSummary.setProsecutingAuthority(PROSECUTING_AUTHORITY);
        caseSummary.setPostingDate(POSTING_DATE);
        caseSearchResult.setId(randomUUID());
        caseSearchResult.setCaseId(CASE_ID);
        caseSearchResult.setCaseSummary(caseSummary);
        caseSearchResult.setCurrentFirstName(FIRST_NAME);
        caseSearchResult.setCurrentLastName(LAST_NAME);
        caseSearchResult.setDateOfBirth(DATE_OF_BIRTH);
        // not resulted
        return caseSearchResult;
    }
}
