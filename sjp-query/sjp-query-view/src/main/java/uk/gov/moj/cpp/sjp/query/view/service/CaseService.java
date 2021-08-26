package uk.gov.moj.cpp.sjp.query.view.service;

import static com.google.common.collect.Iterables.isEmpty;
import static java.lang.Math.ceil;
import static java.util.Arrays.asList;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.accesscontrol.sjp.providers.ProsecutingAuthorityProvider;
import uk.gov.moj.cpp.sjp.domain.common.CaseManagementStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseNotGuiltyPlea;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseWithoutDefendantPostcode;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingCaseToPublishPerOffence;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseApplicationRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDocumentRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;
import uk.gov.moj.cpp.sjp.query.view.ExportType;
import uk.gov.moj.cpp.sjp.query.view.converter.ProsecutingAuthorityAccessFilterConverter;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseDocumentsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseNotGuiltyPleaView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseSearchResultsView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseSummaryView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.response.CaseWithoutDefendantPostcodeView;
import uk.gov.moj.cpp.sjp.query.view.response.CasesMissingSjpnView;
import uk.gov.moj.cpp.sjp.query.view.response.ResultOrdersView;
import uk.gov.moj.cpp.sjp.query.view.response.SearchCaseByMaterialIdView;

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
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.apache.deltaspike.data.api.QueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
public class CaseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CaseService.class);

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private DefendantRepository defendantRepository;

    @Inject
    private CaseDocumentRepository caseDocumentRepository;

    @Inject
    private CaseSearchResultRepository caseSearchResultRepository;

    @Inject
    private CaseApplicationRepository applicationRepository;

    @Inject
    private ProsecutingAuthorityProvider prosecutingAuthorityProvider;

    @Inject
    private ProsecutingAuthorityAccessFilterConverter prosecutingAuthorityAccessFilterConverter;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private ListToJsonArrayConverter<CaseNotGuiltyPleaView> notGuiltyCasesListToJsonArrayConverter;

    @Inject
    private ListToJsonArrayConverter<CaseWithoutDefendantPostcodeView> noPostcodeCaseListToJsonArrayConverter;

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

        final List<CaseSummaryView> cases = casesDetails.stream()
                .map(CaseSummaryView::new).collect(toList());

        final int casesCount = postedBefore
                .map(localDate -> caseRepository.countCasesMissingSjpn(prosecutingAuthorityFilterValue, localDate))
                .orElseGet(() -> caseRepository.countCasesMissingSjpn(prosecutingAuthorityFilterValue));

        return new CasesMissingSjpnView(casesIds, cases, casesCount);
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

    public CaseView findCaseByCorrelationId(final UUID correlationId) {
        try {
            final UUID caseId = defendantRepository.findCaseIdByCorrelationId(correlationId);
            return ofNullable(caseId)
                    .map(id -> getCaseView(caseRepository.findBy(id)))
                    .orElse(null);
        } catch (NoResultException e) {
            LOGGER.debug("No case found with correlationId='{}'", correlationId, e);
            return null;
        }
    }

    public Object findCaseApplicationDecisionId(final UUID applicationDecisionId) {
        try {
            return getCaseView(applicationRepository.findByApplicationDecisionId(applicationDecisionId));
        } catch (NoResultException e) {
            LOGGER.debug("No case found with applicationDecisionId='{}'", applicationDecisionId, e);
            return null;
        }
    }

    public Object findCaseByApplicationId(final UUID applicationId) {
        try {
            return getCaseView(applicationRepository.findByApplicationId(applicationId));
        } catch (NoResultException e) {
            LOGGER.debug("No case found with applicationDecisionId='{}'", applicationId, e);
            return null;
        }
    }

    public SearchCaseByMaterialIdView searchCaseByMaterialId(final UUID materialId) {
        SearchCaseByMaterialIdView searchCaseByMaterialIdView = new SearchCaseByMaterialIdView();
        try {
            final CaseDetail caseDetail = caseRepository.findByMaterialId(materialId);
            if (caseDetail != null) {
                final String prosecutingAuthority = caseDetail.getProsecutingAuthority();
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
        final List<String> WANTED = asList("SJPN", "PLEA", "CITN","APPLICATION");

        return findCaseDocuments(caseId, documentView -> WANTED.contains(documentView.getDocumentType()));
    }

    private CaseView getCaseView(CaseDetail caseDetail) {
        if (null != caseDetail) {
            return getProsecutorDetails(caseDetail.getProsecutingAuthority())
                    .map(object -> new CaseView(caseDetail, object))
                    .orElseGet(() -> new CaseView(caseDetail, null));
        }
        return null;
    }

    private Optional<JsonObject> getProsecutorDetails(final String prosecutingAuthority) {
        return referenceDataService.getProsecutorsByProsecutorCode(prosecutingAuthority)
                .map(prosecutors -> prosecutors.getValuesAs(JsonObject.class))
                .map(prosecutorObjectList -> {
                    if (!prosecutorObjectList.isEmpty()) {
                        return prosecutorObjectList.get(0);
                    } else {
                        return null;
                    }
                });
    }

    public CaseSearchResultsView searchCases(final JsonEnvelope envelope, final String query) {

        final String prosecutingAuthorityFilterValue = prosecutingAuthorityAccessFilterConverter.convertToProsecutingAuthorityAccessFilter(prosecutingAuthorityProvider.getCurrentUsersProsecutingAuthorityAccess(envelope));

        List<CaseSearchResult> searchResults = caseSearchResultRepository.findByUrn(prosecutingAuthorityFilterValue, query);
        if (searchResults.isEmpty()) {
            searchResults = caseSearchResultRepository.findByLastName(prosecutingAuthorityFilterValue, query);
        }
        return new CaseSearchResultsView(searchResults);
    }

    public JsonObject findPendingCasesToPublish(final ExportType exportType) {
        final Map<String, List<PendingCaseToPublishPerOffence>> pendingCasesGroupedByCaseId = getPendingCases(exportType);

        final JsonArrayBuilder pendingCases = createArrayBuilder();
        pendingCasesGroupedByCaseId.forEach((key, value) -> populatePendingCasesArrayBuilder(value, pendingCases));

        return createObjectBuilder().add("pendingCases", pendingCases).build();
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
        return ofNullable(caseRepository.findBy(caseId));
    }

    public JsonObject buildNotGuiltyPleaCasesView(final String prosecutingAuthority, int pageSize, int pageNumber) {

        final Map<String, String> allProsecutors = getAllProsecutorsMap();
        final List<CaseNotGuiltyPlea> results = StringUtils.isEmpty(prosecutingAuthority)
                ? caseRepository.findCasesNotGuiltyPlea()
                : caseRepository.findCasesNotGuiltyPleaByProsecutingAuthority(prosecutingAuthority);

        final int offset = pageSize * (pageNumber - 1);
        final int totalCount = results.size();
        final int pageCount = (int) ceil((double) totalCount / pageSize);

        final List<CaseNotGuiltyPleaView> casesNotGuiltyPleaView = results.stream()
                .skip(offset)
                .limit(pageSize)
                .map(caseNotGuiltyPlea -> new CaseNotGuiltyPleaView(caseNotGuiltyPlea.getId(),
                        caseNotGuiltyPlea.getUrn(),
                        caseNotGuiltyPlea.getPleaDate(),
                        caseNotGuiltyPlea.getFirstName(),
                        caseNotGuiltyPlea.getLastName(),
                        allProsecutors.get(caseNotGuiltyPlea.getProsecutingAuthority()),
                        ofNullable(caseNotGuiltyPlea.getCaseManagementStatus()).orElse(CaseManagementStatus.NOT_STARTED)
                ))
                .collect(toList());

        return buildResponsePayload(casesNotGuiltyPleaView, totalCount, pageCount);

    }

    private JsonObject buildResponsePayload(final List<CaseNotGuiltyPleaView> casesNotGuiltyPlea,
                                            final int totalCount,
                                            final int pageCount) {
        final JsonArray convertedCases = notGuiltyCasesListToJsonArrayConverter.convert(casesNotGuiltyPlea);
        return createObjectBuilder()
                .add("results", totalCount)
                .add("pageCount", pageCount)
                .add("cases", convertedCases)
                .build();
    }

    private JsonObject buildCasesWithoutPostcodeResponsePayload(final List<CaseWithoutDefendantPostcodeView> casesWithoutDefendantPostcode,
                                                                final int totalCount,
                                                                final int pageCount) {
        final JsonArray convertedCases = noPostcodeCaseListToJsonArrayConverter.convert(casesWithoutDefendantPostcode);
        return createObjectBuilder()
                .add("results", totalCount)
                .add("pageCount", pageCount)
                .add("cases", convertedCases)
                .build();
    }

    public Map<String, String> getAllProsecutorsMap() {
        return referenceDataService.getAllProsecutors().map(allProsecutors -> allProsecutors.getValuesAs(JsonObject.class).stream()
                .sorted(comparingInt(prosecutor -> prosecutor.getInt("sequenceNumber")))
                .collect(toMap(prosecutorJson -> prosecutorJson.getString("shortName"),
                        prosecutorJson -> prosecutorJson.getString("fullName"),
                        (existingValue, newValue) -> newValue))
        ).orElse(Collections.emptyMap());
    }

    private void filterOtherAndFinancialMeansDocuments(Collection<CaseDocumentView> caseDocumentsView) {
        Set<String> documentsTypeToRetain = Sets.newHashSet("SJPN", "PLEA", "CITN","APPLICATION");
        caseDocumentsView.removeIf(
                documentView -> !documentsTypeToRetain.contains(documentView.getDocumentType())
        );
    }

    private void populatePendingCasesArrayBuilder(final List<PendingCaseToPublishPerOffence> pendingCaseToPublishPerOffenceList,
                                                  final JsonArrayBuilder pendingCasesArrayBuilder) {

        final PendingCaseToPublishPerOffence theCase = getAnyOffenceForDefendantDetails(pendingCaseToPublishPerOffenceList);

        final Optional<String> town = getTown(theCase);
        final Optional<String> county = getCounty(theCase);
        final Optional<LocalDate> defendantDateOfBirth = getDateOfBirth(theCase);
        final Optional<String> postcode = getPostcode(theCase);
        final JsonArrayBuilder offenceArrayBuilder = createArrayBuilder();
        pendingCaseToPublishPerOffenceList
                .forEach(casePerOffence -> {
                    final JsonObjectBuilder pressRestrictionBuilder = createObjectBuilder()
                            .add("requested", ofNullable(casePerOffence.getPressRestrictionRequested()).orElse(false));
                    ofNullable(casePerOffence.getPressRestrictionName())
                            .ifPresent(pressRestrictionName -> pressRestrictionBuilder.add("name", pressRestrictionName));

                    offenceArrayBuilder.add(createObjectBuilder()
                            .add("offenceCode", casePerOffence.getOffenceCode())
                            .add("offenceStartDate", casePerOffence.getOffenceStartDate().toString())
                            .add("offenceWording", casePerOffence.getOffenceWording())
                            .add("completed", ofNullable(casePerOffence.getCompleted()).orElse(false))
                            .add("pressRestriction", pressRestrictionBuilder));
                });

        final JsonObjectBuilder objectBuilder = createObjectBuilder()
                .add("caseId", theCase.getCaseId().toString())
                .add("caseUrn", theCase.getCaseUrn())
                .add("defendantName", formatName(theCase.getFirstName(), theCase.getLastName()))
                .add("firstName", ofNullable(theCase.getFirstName()).orElse(""))
                .add("lastName", ofNullable(theCase.getLastName()).orElse(""))
                .add("offences", offenceArrayBuilder)
                .add("prosecutorName", theCase.getProsecutor());

        town.ifPresent(t -> objectBuilder.add("town", t));
        county.ifPresent(c -> objectBuilder.add("county", c));
        ofNullable(theCase.getAddressLine1()).ifPresent(line1 -> objectBuilder.add("addressLine1", line1));
        ofNullable(theCase.getAddressLine2()).ifPresent(line2 -> objectBuilder.add("addressLine2", line2));
        defendantDateOfBirth.ifPresent(dob -> objectBuilder.add("defendantDateOfBirth", dob.toString()));
        postcode.ifPresent(p -> objectBuilder.add("postcode", p));
        pendingCasesArrayBuilder.add(objectBuilder);
    }

    private PendingCaseToPublishPerOffence getAnyOffenceForDefendantDetails(final List<PendingCaseToPublishPerOffence> pendingCaseToPublish) {
        return pendingCaseToPublish.get(0);
    }

    private String formatName(final String firstName, final String lastName) {
        final String lastN = ofNullable(lastName).orElse("");
        if (firstName == null || firstName.trim().isEmpty()) {
            return lastN;
        }
        return String.join(" ", firstName.substring(0, 1), lastN).trim();
    }

    private Optional<String> getTown(final PendingCaseToPublishPerOffence pendingCase) {
        final String town = isNotEmpty(pendingCase.getAddressLine5()) ? pendingCase.getAddressLine4() : pendingCase.getAddressLine3();
        return ofNullable(town);
    }

    private Optional<String> getCounty(final PendingCaseToPublishPerOffence pendingCase) {
        final String county = isNotEmpty(pendingCase.getAddressLine5()) ? pendingCase.getAddressLine5() : pendingCase.getAddressLine4();
        return ofNullable(county);
    }

    private Optional<LocalDate> getDateOfBirth(final PendingCaseToPublishPerOffence pendingCaseToPublishWithAnyOffence) {
        return ofNullable(pendingCaseToPublishWithAnyOffence.getDefendantDateOfBirth());
    }



    public JsonObject buildCasesWithoutDefendantPostcodeView(final int pageSize, final int pageNumber) {
        final Map<String, String> allProsecutors = getAllProsecutorsMap();
        final List<CaseWithoutDefendantPostcode> results = caseRepository.findCasesWithoutDefendantPostcode();

        final int offset = pageSize * (pageNumber - 1);
        final int totalCount = results.size();
        final int pageCount = (int) ceil((double) totalCount / pageSize);

        final List<CaseWithoutDefendantPostcodeView> casesWithoutPostcodeView = results.stream()
                .skip(offset)
                .limit(pageSize)
                .map(caseWithoutPostcode -> new CaseWithoutDefendantPostcodeView(
                        caseWithoutPostcode.getId(),
                        caseWithoutPostcode.getUrn(),
                        caseWithoutPostcode.getPostingDate(),
                        caseWithoutPostcode.getFirstName(),
                        caseWithoutPostcode.getLastName(),
                        allProsecutors.get(caseWithoutPostcode.getProsecutingAuthority())
                ))
                .collect(toList());

        return buildCasesWithoutPostcodeResponsePayload(casesWithoutPostcodeView, totalCount, pageCount);
    }
    private Optional<String> getPostcode(final PendingCaseToPublishPerOffence pendingCaseToPublishWithAnyOffence) {
        return  Optional.ofNullable(pendingCaseToPublishWithAnyOffence.getPostcode());
    }

    private Function<PendingCaseToPublishPerOffence, String> caseIdPredicate() {
        return pendingCaseToPublish -> pendingCaseToPublish.getCaseId().toString();
    }

    private Map<String, List<PendingCaseToPublishPerOffence>> getPendingCases(final ExportType exportType) {
        List<PendingCaseToPublishPerOffence> pendingCases;

        if (exportType == ExportType.PUBLIC) {
            pendingCases = caseRepository.findPublicTransparencyReportPendingCases();
        } else {
            pendingCases = caseRepository.findPressTransparencyReportPendingCases();
        }

        return pendingCases.stream().collect(groupingBy(caseIdPredicate()));
    }
}
