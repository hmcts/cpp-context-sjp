package uk.gov.moj.cpp.sjp.query.view.service;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
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
import static uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder.aCase;
import static uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder.aDefendantDetail;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.common.util.Clock;
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
import uk.gov.moj.cpp.sjp.persistence.entity.InterpreterDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseCountByAgeView;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.NotReadyCaseRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseSearchResultsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantsView;
import uk.gov.moj.cpp.sjp.query.view.response.ResultOrdersView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCaseByMaterialIdView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCasesHit;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCasesView;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;


@RunWith(MockitoJUnitRunner.class)
public class CaseServiceTest {

    private static final UUID CASE_ID = randomUUID();
    private static final String URN = "TFL1234";
    private static final String PROSECUTING_AUTHORITY = "prosecutingAuthority";
    private static final Boolean COMPLETED = Boolean.TRUE;
    private static final String INITIATION_CODE = "J";
    private static final String ENTERPRISE_ID = "2K2SLYFC743H";

    private static final UUID ID = randomUUID();
    private static final UUID CASE_ID_TO_FIND = randomUUID();

    private static final String INTERPRETER = "french";
    private static final String PROSECUTING_AUTHORITY_CPS = "CPS";
    private static final String PROSECUTING_AUTHORITY_TFL = "TFL";
    private static final String PROSECUTING_AUTHORITY_TVL = "TVL";
    private static final String PROSECUTING_AUTHORITY_DVLA = "DVLA";

    private static final LocalDate POSTING_DATE = LocalDate.now();
    private static final String FIRST_NAME = "Adam";
    private static final String LAST_NAME = "Zuma";
    private static final String POSTCODE = "AB1 2CD";
    private static final LocalDate DATE_OF_BIRTH = LocalDate.now();

    private Clock clock = new StoppedClock(ZonedDateTime.now(UTC));

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

    @InjectMocks
    private CaseService service;

    @Test
    public void shouldFindCaseViewWithDocumentsWherePostalPlea() {
        assertExpectionsForFindCaseViewWithDocuments(false);
    }

    @Test
    public void shouldFindCaseViewWithDocumentsWhereOnlinePlea() {
        assertExpectionsForFindCaseViewWithDocuments(true);
    }

    private void assertExpectionsForFindCaseViewWithDocuments(boolean onlinePleaReceived) {
        final CaseDetail caseDetail = createCaseDetailWithDocumentTypes(onlinePleaReceived,"FINANCIAL_MEANS", "OTHER", "Travelcard");

        given(caseRepository.findBy(CASE_ID)).willReturn(caseDetail);
        CaseView caseView = service.findCase(CASE_ID.toString());
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
        CaseDetail caseDetail = createCaseDetail(true);

        given(caseRepository.findByUrn(URN)).willReturn(caseDetail);
        CaseView caseView = service.findCaseByUrn(URN);

        assertThat(caseView, notNullValue());
        assertThat(caseView.getId(), is(CASE_ID.toString()));
        assertThat(caseView.getUrn(), is(URN));
        assertThat(caseView.getDateTimeCreated(), is(clock.now()));
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
        CaseView caseView = service.findCaseByUrnPostcode(URN, POSTCODE);

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

    private void shouldSearchCases(final BiFunction<UUID, List<CaseDetail>, Object> setUpFunction,
                                   final Function<String, SearchCasesView> functionToTest) {
        final List<CaseDetail> caseDetailList = new ArrayList<>();
        caseDetailList.add(createCaseDetail());

        final UUID caseId2 = UUID.randomUUID();
        final CaseDetail caseDetailWithPlea = createCaseDetail();
        caseDetailWithPlea.setId(caseId2);
        caseDetailWithPlea.setCompleted(Boolean.FALSE);
        final DefendantDetail defendantDetail = new DefendantDetail();
        final Set<OffenceDetail> offences = new HashSet<>();
        offences.add(OffenceDetail.builder().setPlea("GUILTY").setId(UUID.randomUUID()).build());
        defendantDetail.setOffences(offences);
        caseDetailWithPlea.setDefendant(defendantDetail);
        caseDetailList.add(caseDetailWithPlea);

        setUpFunction.apply(CASE_ID_TO_FIND, caseDetailList);

        SearchCasesView searchCasesView = functionToTest.apply(CASE_ID_TO_FIND.toString());

        assertThat(searchCasesView, notNullValue());

        List<SearchCasesHit> hits = searchCasesView.getHits();
        hits.sort(Comparator.comparing(SearchCasesHit::getPlea));

        assertThat(hits.get(0).getId(), is(CASE_ID.toString()));
        assertThat(hits.get(0).getCompleted(), is(COMPLETED));
        assertThat(hits.get(0).getPlea(), is(""));

        assertThat(hits.get(1).getId(), is(caseDetailWithPlea.getId().toString()));
        assertThat(hits.get(1).getCompleted(), is(Boolean.FALSE));
        assertThat(hits.get(1).getPlea(), is("GUILTY"));
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenTVL() {
        final UUID materialId = UUID.randomUUID();
        CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(CASE_ID);
        caseDetail.setProsecutingAuthority(PROSECUTING_AUTHORITY_TVL);

        when(caseRepository.findByMaterialId(materialId)).thenReturn(caseDetail);

        SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId.toString());

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(CASE_ID.toString()));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(),
                is(ProsecutingAuthority.TVL));
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenDVLA() {
        final UUID materialId = UUID.randomUUID();
        CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(CASE_ID);
        caseDetail.setProsecutingAuthority(PROSECUTING_AUTHORITY_DVLA);

        when(caseRepository.findByMaterialId(materialId)).thenReturn(caseDetail);

        SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId.toString());

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(CASE_ID.toString()));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(),
                is(ProsecutingAuthority.DVLA));
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenTFL() {
        final UUID materialId = UUID.randomUUID();
        CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(CASE_ID);
        caseDetail.setProsecutingAuthority(PROSECUTING_AUTHORITY_TFL);

        when(caseRepository.findByMaterialId(materialId)).thenReturn(caseDetail);

        SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId.toString());

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(CASE_ID.toString()));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(),
                is(ProsecutingAuthority.TFL));
    }

    @Test
    public void shouldSearchCaseByMaterialId_whenCaseIdAndProsecutingAuthorityAreNull() {
        final UUID materialId = UUID.randomUUID();

        when(caseRepository.findByMaterialId(materialId)).thenReturn(null);

        SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId.toString());

        assertThat(searchCaseByMaterialIdView.getCaseId(), nullValue());
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(), nullValue());
    }


    @Test
    public void shouldReturnNullIfCaseNotFoundSearchCaseByMaterialId() {
        final UUID materialId = UUID.randomUUID();

        when(caseRepository.findByMaterialId(materialId)).thenThrow(new NoResultException());

        SearchCaseByMaterialIdView searchCaseByMaterialIdView =
                service.searchCaseByMaterialId(materialId.toString());

        assertThat(searchCaseByMaterialIdView.getCaseId(), is(nullValue()));
        assertThat(searchCaseByMaterialIdView.getProsecutingAuthority(), is(nullValue()));
    }


    @Test
    public void shouldFindCaseDocuments() {
        List<CaseDocument> caseDocumentList = new ArrayList<>();
        UUID documentId = randomUUID();
        CaseDocument caseDocument = new CaseDocument(documentId, randomUUID(), "SJPN", ZonedDateTime.now(), CASE_ID, 2);
        caseDocumentList.add(caseDocument);
        when(caseRepository.findCaseDocuments(CASE_ID)).thenReturn(caseDocumentList);

        CaseDocumentsView caseDocumentsView = service.findCaseDocuments(CASE_ID.toString());
        assertThat(caseDocumentsView, notNullValue());
        CaseDocumentView firstCaseDocument = caseDocumentsView.getCaseDocuments().get(0);
        assertThat(firstCaseDocument.getId().toString(), is(documentId.toString()));
        assertThat(firstCaseDocument.getDocumentNumber(), is(2));
    }

    @Test
    public void shouldFindCaseDocumentsViewWithFilteringOfOtherAndFinancialMeansDocuments() {
        final List<CaseDocument> caseDocuments = createCaseDocuments("FINANCIAL_MEANS", "OTHER", "OTHER-Travelcard");

        given(caseRepository.findCaseDocuments(CASE_ID)).willReturn(caseDocuments);

        final CaseDocumentsView caseDocumentsView = service.findCaseDocumentsFilterOtherAndFinancialMeans(CASE_ID.toString());
        assertThat(caseDocumentsView.getCaseDocuments().size(), is(0));
    }

    @Test
    public void shouldFindCaseDocumentsViewWithFilteringWhichLeavesRequiredDocuments() {
        final List<CaseDocument> caseDocuments = createCaseDocuments("PLEA", "CITN", "SJPN");

        given(caseRepository.findCaseDocuments(CASE_ID)).willReturn(caseDocuments);

        final CaseDocumentsView caseDocumentsView = service.findCaseDocumentsFilterOtherAndFinancialMeans(CASE_ID.toString());
        assertThat(caseDocumentsView.getCaseDocuments().size(), is(3));
    }

    @Test
    public void shouldFindCaseDocumentsViewWithFilteringOfUnwantedDocuments() {
        final List<CaseDocument> caseDocuments = createCaseDocuments("PLEA", "OTHER", "FINANCIAL_MEANS");

        given(caseRepository.findCaseDocuments(CASE_ID)).willReturn(caseDocuments);

        final CaseDocumentsView caseDocumentsView = service.findCaseDocumentsFilterOtherAndFinancialMeans(CASE_ID.toString());
        assertThat(caseDocumentsView.getCaseDocuments().size(), is(1));
    }

    @Test
    public void shouldFindCaseDefendants() {


        Set<OffenceDetail> offences = new HashSet<>();

        DefendantDetail defendant = new DefendantDetail(ID, new PersonalDetails(), offences, 1);
        defendant.setCaseDetail(createCaseDetail());
        defendant.setInterpreter(new InterpreterDetail(INTERPRETER));

        when(caseRepository.findCaseDefendant(CASE_ID)).thenReturn(defendant);

        DefendantsView defendantsView = service.findCaseDefendants(CASE_ID.toString());

        assertThat(defendantsView, notNullValue());

        assertThat(defendantsView.getDefendants().get(0).getInterpreter().getLanguage(), is(INTERPRETER));
        assertThat(defendantsView.getDefendants().get(0).getCaseId(), is(CASE_ID));
        assertThat(defendantsView.getDefendants().get(0).getId(), is(ID));
        assertThat(defendantsView.getDefendants().get(0).getOffences().size(), is(0));

    }

    @Test
    public void shouldFindAwatingCases() {

        final CaseDetail caseDetail =
                aCase().addDefendantDetail(aDefendantDetail().build()).build();
        when(caseRepository.findAwaitingSjpCases(600)).thenReturn(asList(caseDetail));

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

        DefendantDetail defendantDetail = new DefendantDetail(UUID.randomUUID(), new PersonalDetails(), null, 0);
        CaseDetail caseDetail = new CaseDetail(UUID.randomUUID(), "TFL1234", null, null, null, null, null, defendantDetail, null, null);

        CaseDocument caseDocument = new CaseDocument(UUID.randomUUID(),
                UUID.randomUUID(), CaseDocument.RESULT_ORDER_DOCUMENT_TYPE,
                ZonedDateTime.now(), caseDetail.getId(), null);

        final ZonedDateTime FROM_DATE_TIME = FROM_DATE.atStartOfDay(ZoneOffset.UTC);
        final ZonedDateTime TO_DATE_TIME = TO_DATE.atStartOfDay(ZoneOffset.UTC);
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

        DefendantDetail defendantDetail = new DefendantDetail(UUID.randomUUID(), new PersonalDetails(), null, 0);
        CaseDetail caseDetail = new CaseDetail(UUID.randomUUID(), "TFL1234", null, null, null, null, null, defendantDetail, null, null);
        CaseDocument caseDocument = new CaseDocument(UUID.randomUUID(),
                UUID.randomUUID(), CaseDocument.RESULT_ORDER_DOCUMENT_TYPE,
                ZonedDateTime.now(), caseDetail.getId(), null);

        final ZonedDateTime FROM_DATE_TIME = FROM_DATE.atStartOfDay(ZoneOffset.UTC);
        final ZonedDateTime TO_DATE_TIME = TO_DATE.atStartOfDay(ZoneOffset.UTC);
        when(caseDocumentRepository.findCaseDocumentsOrderedByAddedByDescending(FROM_DATE_TIME,
                TO_DATE_TIME, CaseDocument.RESULT_ORDER_DOCUMENT_TYPE)).thenReturn(
                Arrays.asList(caseDocument));
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
                .thenReturn(asList(createCaseSearchResult()));

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
                .thenReturn(asList(createCaseSearchResult()));

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

    private CaseDetail createCaseDetail() {
        return createCaseDetail(false);
    }

    private CaseDetail createCaseDetail(boolean onlinePleaReceived) {
        CaseDetail caseDetail = new CaseDetail(CASE_ID, URN, PROSECUTING_AUTHORITY_CPS,
                null, COMPLETED, null, clock.now(), new DefendantDetail(), null, null);
        caseDetail.setOnlinePleaReceived(onlinePleaReceived);
        return caseDetail;
    }

    private CaseDetail createCaseDetailWithDocumentTypes(String... documentTypes) {
        return createCaseDetailWithDocumentTypes(false, documentTypes);
    }

    private CaseDetail createCaseDetailWithDocumentTypes(boolean onlinePlea, String... documentTypes) {
        final CaseDetail caseDetail = createCaseDetail(onlinePlea);
        createCaseDocuments(documentTypes).forEach(caseDetail::addCaseDocuments);
        return caseDetail;
    }

    private List<CaseDocument> createCaseDocuments(String... documentTypes) {
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
        caseSummary.setInitiationCode(INITIATION_CODE);
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
