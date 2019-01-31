package uk.gov.moj.cpp.sjp.query.view.service;

import static com.google.common.collect.Iterables.isEmpty;
import static java.util.Arrays.asList;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.persistence.entity.AwaitingCase;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseSearchResultsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.CasesMissingSjpnView;
import uk.gov.moj.cpp.sjp.query.view.response.ResultOrdersView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCaseByMaterialIdView;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import com.google.common.collect.Sets;
import org.apache.deltaspike.data.api.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class CaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseService.class);
    @Inject
    private CaseRepository caseRepository;

    @Inject
    private CaseDocumentRepository caseDocumentRepository;

    @Inject
    private CaseSearchResultRepository caseSearchResultRepository;

    @Inject
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Inject
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    /**
     * Find case by id.
     *
     * @param id id of the case to find.
     * @return CaseView. Null, when not found.
     */
    public CaseView findCase(final UUID id) {
        return getCaseView(caseRepository.findBy(id));
    }

    public CaseView findCaseAndFilterOtherAndFinancialMeansDocuments(String caseId) {
        final CaseView caseView = getCaseView(caseRepository.findBy(fromString(caseId)));
        if (caseView != null && !isEmpty(caseView.getCaseDocuments())) {
            filterOtherAndFinancialMeansDocuments(caseView.getCaseDocuments());
        }
        return caseView;
    }

    public CasesMissingSjpnView findCasesMissingSjpn(final JsonEnvelope envelope,
                                                     final Optional<Integer> limit,
                                                     final Optional<LocalDate> postedBefore) {

        final List<CaseDetail> casesDetails;

        final String prosecutingAuthorityFilterValue = prosecutingAuthorityAccessFilterConverter
                .convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityProvider
                        .getCurrentUsersProsecutingAuthorityAccess(envelope));

        if (limit.isPresent() && limit.get() < 1) {
            casesDetails = Collections.emptyList();
        } else {
            QueryResult<CaseDetail> caseDetailsResult;
            if (postedBefore.isPresent()) {
                caseDetailsResult = caseRepository.findCasesMissingSjpn(prosecutingAuthorityFilterValue, postedBefore.get());
            } else {
                caseDetailsResult = caseRepository.findCasesMissingSjpn(prosecutingAuthorityFilterValue);
            }

            if (limit.isPresent()) {
                casesDetails = caseDetailsResult.maxResults(limit.get()).getResultList();
            } else {
                casesDetails = caseDetailsResult.getResultList();
            }
        }

        final List<String> casesIds = casesDetails.stream()
                .map(caseDetails -> caseDetails.getId().toString()).collect(toList());
        final int casesCount = postedBefore
                .map(localDate -> caseRepository.countCasesMissingSjpn(prosecutingAuthorityFilterValue, localDate))
                .orElseGet(() -> caseRepository.countCasesMissingSjpn(prosecutingAuthorityFilterValue));

        return new CasesMissingSjpnView(casesIds, casesCount);
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

    public CaseView findCaseByUrnPostcode(final String urn, final String postcode) {
        try {
            return getCaseView(caseRepository.findByUrnPostcode(urn, postcode));
        } catch (NonUniqueResultException e) {
            LOGGER.warn("Multiple cases found for URN (ignoring prefix) and postcode. URN='{}', postcode='{}'", urn, postcode, e);
            return null;
        }
    }

    public SearchCaseByMaterialIdView searchCaseByMaterialId(final UUID materialId) {
        SearchCaseByMaterialIdView searchCaseByMaterialIdView = new SearchCaseByMaterialIdView();
        try {
            final CaseDetail caseDetail = caseRepository.findByMaterialId(materialId);
            if (caseDetail != null) {
                final ProsecutingAuthority prosecutingAuthority = caseDetail.getProsecutingAuthority();
                searchCaseByMaterialIdView = new SearchCaseByMaterialIdView(caseDetail.getId(), prosecutingAuthority);
            } else {
                searchCaseByMaterialIdView = new SearchCaseByMaterialIdView(null, null);
            }
        } catch (NoResultException e) {
            LOGGER.error("No case found with materialId='{}'", materialId, e);
        }
        return searchCaseByMaterialIdView;
    }

    /**
     * Find case documents
     *
     * @param caseId id of the case
     * @return case documents for the case
     */
    public CaseDocumentsView findCaseDocuments(final UUID caseId) {
        return findCaseDocuments(caseId, d -> true);
    }

    public Optional<CaseDocumentView> findCaseDocument(final UUID caseId, final UUID documentId) {
        return findCaseDocuments(caseId, document -> document.getId().equals(documentId))
                .getCaseDocuments().stream().findFirst();
    }

    private CaseDocumentsView findCaseDocuments(final UUID caseId, final Predicate<CaseDocument> filter) {
        return caseRepository.findCaseDocuments(caseId)
                .stream()
                .filter(filter)
                .map(CaseDocumentView::new)
                .sorted(CaseDocumentView.BY_DOCUMENT_TYPE_AND_NUMBER)
                .collect(Collectors.collectingAndThen(toList(), CaseDocumentsView::new));
    }

    public CaseDocumentsView findCaseDocumentsFilterOtherAndFinancialMeans(UUID caseId) {
        final List<String> WANTED = asList("SJPN", "PLEA", "CITN");

        return findCaseDocuments(caseId, documentView -> WANTED.contains(documentView.getDocumentType()));
    }

    private CaseView getCaseView(CaseDetail caseDetail) {
        if (null != caseDetail) {
            return new CaseView(caseDetail);
        }
        return null;
    }

    public CaseSearchResultsView searchCases(final JsonEnvelope envelope, final String query) {

        final String prosecutingAuthorityFilterValue = prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope));

        List<CaseSearchResult> searchResults = caseSearchResultRepository.findByUrn(prosecutingAuthorityFilterValue, query);
        if (searchResults.isEmpty()) {
            searchResults = caseSearchResultRepository.findByLastName(prosecutingAuthorityFilterValue, query);
        }
        return new CaseSearchResultsView(searchResults);
    }

    public JsonObject findAwaitingCases() {

        final List<AwaitingCase> awaitingSjpCases = caseRepository.findAwaitingSjpCases(600);

        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        awaitingSjpCases.forEach(awaitingCase ->
            arrayBuilder.add(createObjectBuilder()
                    .add("firstName", awaitingCase.getDefendantFirstName())
                    .add("lastName", awaitingCase.getDefendantLastName())
                    .add("offenceCode", awaitingCase.getOffenceCode())));
        return createObjectBuilder().add("awaitingCases", arrayBuilder).build();
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
                            .setDefendant(caseDetail.getDefendant().getPersonalDetails())
                            .setOrder(caseDocument.getId(), caseDocument.getAddedAt())
                            .build());
        };

        resultOrders.stream().filter(e -> caseRepository.findBy(e.getCaseId()) != null).forEach(caseDocumentConsumer);

        return resultOrdersView;
    }

    public Optional<CaseDetail> getCase(final UUID caseId) {
        return Optional.ofNullable(caseRepository.findBy(caseId));
    }

    private void filterOtherAndFinancialMeansDocuments(Collection<CaseDocumentView> caseDocumentsView) {
        Set<String> documentsTypeToRetain = Sets.newHashSet("SJPN", "PLEA", "CITN");
        caseDocumentsView.removeIf(
                documentView -> !documentsTypeToRetain.contains(documentView.getDocumentType())
        );
    }
}
