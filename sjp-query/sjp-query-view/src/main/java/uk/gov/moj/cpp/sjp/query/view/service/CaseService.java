package uk.gov.moj.cpp.sjp.query.view.service;

import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Range.atMost;
import static com.google.common.collect.Range.closed;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.toJsonArray;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetailMissingSjpn;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSummary;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseCountByAgeView;
import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseReferredToCourt;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseReferredToCourtRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.NotReadyCaseRepository;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseMissingSjpnWithDetailsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.CasesMissingSjpnView;
import uk.gov.moj.cpp.sjp.query.view.response.CasesMissingSjpnWithDetailsView;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantView;
import uk.gov.moj.cpp.sjp.query.view.response.DefendantsView;
import uk.gov.moj.cpp.sjp.query.view.response.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.query.view.response.ResultOrdersView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCaseByMaterialIdView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCasesHit;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCasesView;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.NoResultException;

import com.google.common.collect.Range;
import com.google.common.collect.Sets;
import org.apache.deltaspike.data.api.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class CaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseService.class);

    private static final int NOTICE_PERIOD = 21;
    private static final int NOTICE_DELIVERY_TOLERANCE = 7;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private CaseDocumentRepository caseDocumentRepository;

    @Inject
    private NotReadyCaseRepository notReadyCaseRepository;

    @Inject
    private CaseSearchResultRepository caseSearchResultRepository;

    @Inject
    private CaseReferredToCourtRepository caseReferredToCourtRepository;

    /**
     * Find case by id.
     *
     * @param id id of the case to find.
     * @return CaseView. Null, when not found.
     */
    public CaseView findCase(final String id) {
        return getCaseView(caseRepository.findBy(UUID.fromString(id)));
    }

    public CaseView findCaseAndFilterOtherAndFinancialMeansDocuments(String caseId) {
        final CaseView caseView = getCaseView(caseRepository.findBy(UUID.fromString(caseId)));
        if (caseView != null && !isEmpty(caseView.getCaseDocuments())) {
            filterOtherAndFinancialMeansDocuments(caseView.getCaseDocuments());
        }
        return caseView;
    }

    /**
     * Find cases missing the SJP notice document.
     *
     * @param limit limit the number of IDs returned
     * @return CasesMissingSjpnView
     */
    public CasesMissingSjpnView findCasesMissingSjpn(final Optional<Integer> limit, final Optional<LocalDate> postedBefore) {

        final List<CaseDetail> casesDetails;

        if (limit.isPresent() && limit.get() < 1) {
            casesDetails = Collections.emptyList();
        } else {
            QueryResult<CaseDetail> caseDetailsResult;
            if (postedBefore.isPresent()) {
                caseDetailsResult = caseRepository.findCasesMissingSjpn(postedBefore.get());
            } else {
                caseDetailsResult = caseRepository.findCasesMissingSjpn();
            }

            if (limit.isPresent()) {
                casesDetails = caseDetailsResult.maxResults(limit.get()).getResultList();
            } else {
                casesDetails = caseDetailsResult.getResultList();
            }
        }

        final List<String> casesIds = casesDetails.stream().map(caseDetails -> caseDetails.getId().toString()).collect(toList());
        final int casesCount = postedBefore.map(caseRepository::countCasesMissingSjpn).orElseGet(caseRepository::countCasesMissingSjpn);

        return new CasesMissingSjpnView(casesIds, casesCount);
    }

    /**
     * Find cases missing the SJP notice document with additional details.
     *
     * @param postedBefore only find docs posted before this number of days
     * @return CasesMissingSjpnWithDetailsView
     */
    public CasesMissingSjpnWithDetailsView findCasesMissingSjpnWithDetails(final Optional<LocalDate> postedBefore) {

        List<CaseDetailMissingSjpn> caseDetailMissingSjpnResult;
        if (postedBefore.isPresent()) {
            caseDetailMissingSjpnResult = caseRepository.findCasesMissingSjpnWithDetails(postedBefore.get());
        } else {
            caseDetailMissingSjpnResult = caseRepository.findCasesMissingSjpnWithDetails();
        }
        final int casesCount = caseDetailMissingSjpnResult.size();

        return new CasesMissingSjpnWithDetailsView(
                caseDetailMissingSjpnResult.stream().map(CaseMissingSjpnWithDetailsView::new).collect(toList()),
                casesCount);
    }

    /**
     * @param urn urn of the case to find.
     * @return CaseView. Null, when not found.
     */
    public CaseView findCaseByUrn(final String urn) {
        try {
            return getCaseView(caseRepository.findByUrn(urn));
        } catch (NoResultException e) {
            LOGGER.debug("No case found with URN='{}'", urn, e);
            return null;
        }
    }

    public CaseView findSjpCaseByUrn(final String urn) {
        return getCaseView(caseRepository.findSjpCaseByUrn(urn));
    }

    /**
     * Search case by personId.
     *
     * @param personId id of the defendant person to find cases for.
     * @return SearchView containing matched case summaries
     */
    public SearchCasesView searchCasesByPersonId(final String personId) {
        final List<SearchCasesHit> cases = caseRepository.findByPersonId(UUID.fromString(personId))
                .stream().map(SearchCasesHit::new).collect(toList());
        return new SearchCasesView(personId, cases);
    }

    public SearchCaseByMaterialIdView searchCaseByMaterialId(final String q) {
        SearchCaseByMaterialIdView searchCaseByMaterialIdView = new SearchCaseByMaterialIdView();
        try {
            CaseDetail caseDetail = caseRepository.findByMaterialId(UUID.fromString(q));
            if (caseDetail != null) {
                String caseId = caseDetail.getId().toString();
                ProsecutingAuthority prosecutingAuthority = ProsecutingAuthority.valueOf(caseDetail.getProsecutingAuthority());
                searchCaseByMaterialIdView = new SearchCaseByMaterialIdView(caseId, prosecutingAuthority);
            } else {
                searchCaseByMaterialIdView = new SearchCaseByMaterialIdView(null, null);
            }
        } catch (NoResultException e) {
            LOGGER.error("No case found with materialId='{}'", q, e);
        }
        return searchCaseByMaterialIdView;
    }


    /**
     * Find case documents
     *
     * @param caseId id of the case
     * @return case documents for the case
     */
    public CaseDocumentsView findCaseDocuments(final String caseId) {
        final List<CaseDocumentView> caseDocuments = caseRepository.findCaseDocuments(UUID.fromString(caseId)).stream().map(CaseDocumentView::new).collect(toList());
        caseDocuments.sort(CaseDocumentView.BY_DOCUMENT_TYPE_AND_NUMBER);
        return new CaseDocumentsView(caseDocuments);
    }

    public CaseDocumentsView findCaseDocumentsFilterOtherAndFinancialMeans(String caseId) {
        List<CaseDocumentView> caseDocuments = caseRepository.findCaseDocuments(UUID.fromString(caseId)).stream().map(CaseDocumentView::new).collect(toList());
        caseDocuments.sort(CaseDocumentView.BY_DOCUMENT_TYPE_AND_NUMBER);
        final CaseDocumentsView caseDocumentsView = new CaseDocumentsView(caseDocuments);
        filterOtherAndFinancialMeansDocuments(caseDocumentsView.getCaseDocuments());
        return caseDocumentsView;
    }

    /**
     * Find case defendants
     *
     * @param caseId id of the case
     * @return case defendants for the case
     */
    public DefendantsView findCaseDefendants(final String caseId) {
        List<DefendantView> caseDefendant =
                caseRepository.findCaseDefendants(UUID.fromString(caseId)).stream()
                        .map(DefendantView::new).collect(toList());
        return new DefendantsView(caseDefendant);
    }

    private CaseView getCaseView(CaseDetail caseDetail) {
        if (null != caseDetail) {
            return new CaseView(caseDetail);
        }
        return null;
    }

    public JsonObject searchCases(final String query) {
        List<CaseSearchResult> searchResults = caseSearchResultRepository.findByCaseSummary_urn(query);
        if (searchResults.isEmpty()) {
            searchResults = caseSearchResultRepository.findByLastName(query);
        }
        final JsonArray results = toJsonArray(searchResults, this::convertCaseSearchResult);
        return createObjectBuilder().add("results", results).build();
    }

    public JsonObject findAwaitingCases() {

        final List<CaseDetail> awaitingSjpCases = caseRepository.findAwaitingSjpCases(600);

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        awaitingSjpCases.forEach(sjpCase -> {
            final DefendantDetail defendant = sjpCase.getDefendants().iterator().next();
            arrayBuilder.add(createObjectBuilder()
                    .add("personId", defendant.getPersonId().toString())
                    .add("offenceCode", defendant.getOffences().iterator().next().getCode()));
        });
        return createObjectBuilder().add("awaitingCases", arrayBuilder).build();
    }

    public JsonObject findCasesReferredToCourt() {
        final List<CaseReferredToCourt> unactionedCases = caseReferredToCourtRepository.findUnactionedCases();
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        unactionedCases.forEach(sjpCase -> {
            final JsonObjectBuilder objectBuilder = createObjectBuilder()
                    .add("caseId", sjpCase.getCaseId().toString())
                    .add("urn", sjpCase.getUrn())
                    .add("firstName", sjpCase.getFirstName())
                    .add("lastName", sjpCase.getLastName())
                    .add("hearingDate", sjpCase.getHearingDate().toString());
            if (sjpCase.getInterpreterLanguage() != null) {
                objectBuilder.add("interpreterLanguage", sjpCase.getInterpreterLanguage());
            }
            arrayBuilder.add(objectBuilder);
        });
        return createObjectBuilder().add("cases", arrayBuilder).build();
    }

    public ResultOrdersView findResultOrders(LocalDate fromDate, LocalDate toDate) {

        final ResultOrdersView resultOrdersView = new ResultOrdersView();

        final List<CaseDocument> resultOrders = caseDocumentRepository
                .findCaseDocumentsOrderedByAddedByDescending(
                        fromDate.atStartOfDay(ZoneOffset.UTC),
                        toDate.atStartOfDay(ZoneOffset.UTC),
                        CaseDocument.RESULT_ORDER_DOCUMENT_TYPE);

        Consumer<CaseDocument> caseDocumentConsumer = caseDocument -> {
            CaseDetail caseDetail = caseRepository.findBy(caseDocument.getCaseId());
            resultOrdersView.addResultOrder(
                    new ResultOrdersView.ResultOrderView.Builder()
                            .setCaseId(caseDocument.getCaseId())
                            .setUrn(caseDetail.getUrn())
                            .setDefendant(caseDetail.getDefendants().stream()
                                    .findFirst().get().getPersonId())
                            .setOrder(caseDocument.getMaterialId(), caseDocument.getAddedAt())
                            .build());
        };


        resultOrders.stream().filter(e -> caseRepository.findBy(e.getCaseId()) != null).forEach(caseDocumentConsumer);

        return resultOrdersView;
    }

    public JsonObject getNotReadyCasesGroupedByAge() {
        final List<CaseCountByAgeView> caseCountByAgeViews = notReadyCaseRepository.getCountOfCasesByAge();

        final List<Range<Integer>> ageRanges = asList(atMost(NOTICE_PERIOD - 1), closed(NOTICE_PERIOD, NOTICE_PERIOD + NOTICE_DELIVERY_TOLERANCE - 1));

        final Map<Range<Integer>, Long> caseCountsByAgeRange = caseCountByAgeViews.stream().collect(
                groupingBy(
                        caseCountByAge -> ageRanges.stream().filter(range -> range.contains(caseCountByAge.getAge())).findFirst().orElse(null),
                        summingLong(CaseCountByAgeView::getCount)
                ));

        final JsonArrayBuilder caseCountsByAgeRanges = createArrayBuilder();

        for (final Map.Entry<Range<Integer>, Long> casesCountByAgeRange : caseCountsByAgeRange.entrySet()) {

            final Range<Integer> ageRange = casesCountByAgeRange.getKey();
            final Long casesCount = casesCountByAgeRange.getValue();

            final JsonObjectBuilder casesCountInAgeRange = createObjectBuilder();
            if (ageRange.hasLowerBound()) {
                casesCountInAgeRange.add("ageFrom", ageRange.lowerEndpoint());
            }
            if (ageRange.hasUpperBound()) {
                casesCountInAgeRange.add("ageTo", ageRange.upperEndpoint());
            }
            casesCountInAgeRange.add("casesCount", casesCount);
            caseCountsByAgeRanges.add(casesCountInAgeRange.build());
        }

        return createObjectBuilder().add("caseCountsByAgeRanges", caseCountsByAgeRanges).build();
    }

    public JsonObject getOldestCaseAge() {
        final LocalDate oldestUncompletedPostingDate = caseRepository.findOldestUncompletedPostingDate();
        final long age = oldestUncompletedPostingDate == null ? 0 : DAYS.between(oldestUncompletedPostingDate, LocalDate.now());
        return createObjectBuilder().add("oldestCaseAge", age).build();
    }

    private JsonObject convertCaseSearchResult(final CaseSearchResult searchResult) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add("id", searchResult.getId().toString())
                .add("caseId", searchResult.getCaseId().toString())
                .add("personId", searchResult.getPersonId().toString())
                .add("lastName", searchResult.getLastName());
        // it may be possible that the person details are added before the case is created
        final CaseSummary caseSummary = searchResult.getCaseSummary();
        if (caseSummary != null) {
            objectBuilder.add("urn", caseSummary.getUrn())
                    .add("initiationCode", caseSummary.getInitiationCode())
                    .add("prosecutingAuthority", caseSummary.getProsecutingAuthority())
                    .add("postingDate", caseSummary.getPostingDate().toString())
                    .add("completed", caseSummary.isCompleted());
            // enterpriseId is added after the case is created
            if (caseSummary.getEnterpriseId() != null) {
                objectBuilder.add("enterpriseId", caseSummary.getEnterpriseId());
            }

            if (caseSummary.getReopenedDate() != null) {
                objectBuilder.add("reopenedDate", caseSummary.getReopenedDate().toString());
            }
        }

        if (searchResult.getPleaDate() != null) {
            objectBuilder.add("pleaDate", searchResult.getPleaDate().toString());
        }
        if (searchResult.getWithdrawalRequestedDate() != null) {
            objectBuilder.add("withdrawalRequestedDate", searchResult.getWithdrawalRequestedDate().toString());
        }
        if (searchResult.getFirstName() != null) {
            objectBuilder.add("firstName", searchResult.getFirstName());
        }
        if (searchResult.getDateOfBirth() != null) {
            objectBuilder.add("dateOfBirth", searchResult.getDateOfBirth().toString());
        }
        if (searchResult.getPostCode() != null) {
            objectBuilder.add("postCode", searchResult.getPostCode());
        }
        return objectBuilder.build();
    }

    private void filterOtherAndFinancialMeansDocuments(Collection<CaseDocumentView> caseDocumentsView) {
        Set<String> documentsTypeToRetain = Sets.newHashSet("SJPN", "PLEA", "CITN");
        caseDocumentsView.removeIf(
                documentView -> !documentsTypeToRetain.contains(documentView.getDocumentType())
        );
    }
}
