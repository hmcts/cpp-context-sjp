package uk.gov.moj.cpp.sjp.query.view;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Optional.empty;
import static java.util.UUID.fromString;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.query.view.util.JsonUtility.getString;

import uk.gov.justice.services.common.converter.ListToJsonArrayConverter;
import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePlea;
import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OffenceRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaDetailRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.sjp.query.view.response.CaseNotGuiltyPleaView;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;
import uk.gov.moj.cpp.sjp.query.view.service.DatesToAvoidService;
import uk.gov.moj.cpp.sjp.query.view.service.DefendantService;
import uk.gov.moj.cpp.sjp.query.view.service.EmployerService;
import uk.gov.moj.cpp.sjp.query.view.service.FinancialMeansService;
import uk.gov.moj.cpp.sjp.query.view.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.query.view.service.TransparencyReportService;
import uk.gov.moj.cpp.sjp.query.view.service.UserAndGroupsService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.QUERY_VIEW)
public class SjpQueryView {

    static final String FIELD_CASE_ID = "caseId";
    static final String FIELD_URN = "urn";
    static final String FIELD_POSTCODE = "postcode";
    static final String FIELD_QUERY = "q";
    static final String FIELD_DEFENDANT_ID = "defendantId";
    static final String FIELD_DAYS_SINCE_POSTING = "daysSincePosting";


    private static final String NAME_RESPONSE_CASE = "sjp.query.case-response";
    private static final String NAME_RESPONSE_CASES_SEARCH = "sjp.query.cases-search-response";
    private static final String NAME_RESPONSE_CASES_SEARCH_BY_MATERIAL_ID = "sjp.query.cases-search-by-material-id-response";
    private static final String NAME_RESPONSE_CASE_DOCUMENTS = "sjp.query.case-documents-response";
    private static final String NAME_RESPONSE_PENDING_CASES = "sjp.query.pending-cases";
    private static final String TRANSPARENCY_REPORT_METADATA_RESPONSE_NAME = "sjp.query.transparency-report-metadata";
    private static final String NOT_GUILTY_PLEA_CASES_RESPONSE_NAME = "sjp.query.not-guilty-plea-cases";

    @Inject
    private CaseService caseService;

    @Inject
    private DefendantService defendantService;

    @Inject
    private TransparencyReportService transparencyReportService;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private FinancialMeansService financialMeansService;

    @Inject
    private EmployerService employerService;

    @Inject
    private DatesToAvoidService datesToAvoidService;

    @Inject
    private UserAndGroupsService userAndGroupsService;

    @Inject
    private OnlinePleaRepository.FinancialMeansOnlinePleaRepository onlinePleaRepository;

    @Inject
    private OnlinePleaDetailRepository onlinePleaDetailRepository;

    @Inject
    private OffenceRepository offenceRepository;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private Enveloper enveloper;

    @Inject
    private ListToJsonArrayConverter<CaseNotGuiltyPleaView> listToJsonArrayConverter;

    @Handles("sjp.query.case")
    public JsonEnvelope findCase(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE).apply(
                caseService.findCase(fromString(extract(envelope, FIELD_CASE_ID))));
    }

    @Handles("sjp.query.case-filter-other-and-financial-means-documents")
    public JsonEnvelope findCaseAndFilterOtherAndFinancialMeansDocuments(JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE)
                .apply(caseService.findCaseAndFilterOtherAndFinancialMeansDocuments(extract(envelope, FIELD_CASE_ID)));
    }


    @Handles("sjp.query.case-by-urn")
    public JsonEnvelope findCaseByUrn(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE).apply(
                caseService.findCaseByUrn(extract(envelope, FIELD_URN)));
    }

    @Handles("sjp.query.case-by-urn-postcode")
    public JsonEnvelope findCaseByUrnPostcode(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE).apply(
                caseService.findCaseByUrnPostcode(extract(envelope, FIELD_URN),
                        extract(envelope, FIELD_POSTCODE)));
    }

    @Handles("sjp.query.case-search-results")
    public JsonEnvelope findCaseSearchResults(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASES_SEARCH).apply(
                caseService.searchCases(envelope, extract(envelope, FIELD_QUERY)));
    }

    @Handles("sjp.query.cases-missing-sjpn")
    public JsonEnvelope findCasesMissingSjpn(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final Optional<Integer> limit = payload.containsKey("limit") ? Optional.of(payload.getInt("limit")) : empty();
        final Optional<LocalDate> postedBefore = payload.containsKey(FIELD_DAYS_SINCE_POSTING) ?
                Optional.of(LocalDate.now().minusDays(payload.getInt(FIELD_DAYS_SINCE_POSTING))) : empty();

        return enveloper.withMetadataFrom(envelope, "sjp.query.cases-missing-sjpn")
                .apply(caseService.findCasesMissingSjpn(envelope, limit, postedBefore));
    }

    @Handles("sjp.query.case-documents")
    public JsonEnvelope findCaseDocuments(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE_DOCUMENTS).apply(
                caseService.findCaseDocuments(fromString(extract(envelope, FIELD_CASE_ID))));
    }

    @Handles("sjp.query.case-documents-filter-other-and-financial-means")
    public JsonEnvelope findCaseDocumentsFilterOtherAndFinancialMeans(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE_DOCUMENTS).apply(
                caseService.findCaseDocumentsFilterOtherAndFinancialMeans(fromString(extract(envelope, FIELD_CASE_ID))));

    }

    @Handles("sjp.query.financial-means")
    public JsonEnvelope findFinancialMeans(final JsonEnvelope envelope) {
        final UUID defendantId = fromString(extract(envelope, FIELD_DEFENDANT_ID));
        final Optional<FinancialMeans> financialMeans = financialMeansService.getFinancialMeans(defendantId);
        return enveloper.withMetadataFrom(envelope, "sjp.query.financial-means")
                .apply(financialMeans.orElseGet(() -> new FinancialMeans(null, null, null, null)));
    }

    @Handles("sjp.query.employer")
    public JsonEnvelope findEmployer(final JsonEnvelope envelope) {
        final UUID defendantId = fromString(extract(envelope, FIELD_DEFENDANT_ID));
        final Optional<Employer> employer = employerService.getEmployer(defendantId);
        return enveloper.withMetadataFrom(envelope, "sjp.query.employer")
                .apply(employer.orElseGet(() -> new Employer(null, null, null, null, null)));
    }

    @Handles("sjp.query.cases-search-by-material-id")
    public JsonEnvelope searchCaseByMaterialId(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASES_SEARCH_BY_MATERIAL_ID).apply(
                caseService.searchCaseByMaterialId(fromString(extract(envelope, FIELD_QUERY))));

    }

    @Handles("sjp.query.pending-cases")
    public JsonEnvelope getPendingCasesToPublish(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_PENDING_CASES).apply(
                caseService.findPendingCasesToPublish());
    }

    @Handles("sjp.query.result-orders")
    public JsonEnvelope getResultOrders(final JsonEnvelope envelope) {
        final LocalDate fromDate = LocalDates.from(extract(envelope, "fromDate"));
        final LocalDate toDate = LocalDates.from(extract(envelope, "toDate"));

        return enveloper.withMetadataFrom(envelope, "sjp.query.result-orders")
                .apply(caseService.findResultOrders(fromDate, toDate));
    }

    @Handles("sjp.query.defendants-online-plea")
    public JsonEnvelope findDefendantsOnlinePlea(JsonEnvelope envelope) {
        final UUID caseId = fromString(extract(envelope, FIELD_CASE_ID));
        final UUID defendantId = fromString(extract(envelope, FIELD_DEFENDANT_ID));
        OnlinePlea onlinePlea;
        if (userAndGroupsService.canSeeOnlinePleaFinances(envelope)) {
            onlinePlea = onlinePleaRepository.findBy(caseId);
        } else {
            // Prosecutors cannot see finances.
            onlinePlea = onlinePleaRepository.findOnlinePleaWithoutFinances(caseId);
        }

        final List<OnlinePleaDetail> onlinePleaDetails = onlinePleaDetailRepository.findByCaseIdAndDefendantId(caseId, defendantId);

        onlinePleaDetails
                .stream()
                .forEach(onlinePleaDetail -> {
                    final OffenceDetail offenceDetail = offenceRepository.findBy(onlinePleaDetail.getOffenceId());
                    referenceDataService.getOffenceData(offenceDetail.getCode()).
                        ifPresent(offenceRefData -> {
                            final String title = nonNull(offenceRefData.getString("title", null)) ? offenceRefData.getString("title") : offenceRefData.getString("titleWelsh", null);
                            onlinePleaDetail.setOffenceTitle(title);
                        });
                });

        onlinePlea.setOnlinePleaDetails(onlinePleaDetails);

        return enveloper.withMetadataFrom(envelope, "sjp.query.defendants-online-plea").apply(onlinePlea);
    }


    @Handles("sjp.query.pending-dates-to-avoid")
    public JsonEnvelope findPendingDatesToAvoid(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, "sjp.pending-dates-to-avoid")
                .apply(datesToAvoidService.findCasesPendingDatesToAvoid(envelope));
    }

    @Handles("sjp.query.case-prosecuting-authority")
    public JsonEnvelope getProsecutingAuthority(final JsonEnvelope query) {
        final UUID caseId = fromString(query.payloadAsJsonObject().getString(FIELD_CASE_ID));

        final JsonObject prosecutingAuthorityPayload = Optional.ofNullable(caseRepository.getProsecutingAuthority(caseId))
                .map(prosecutingAuthority -> createObjectBuilder().add("prosecutingAuthority", prosecutingAuthority).build())
                .orElse(null);

        return enveloper.withMetadataFrom(query, "sjp.query.case-prosecuting-authority").apply(prosecutingAuthorityPayload);
    }

    @Handles("sjp.query.defendant-details-updates")
    public JsonEnvelope findDefendantDetailUpdates(JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, "sjp.query.defendant-details-updates")
                .apply(defendantService.findDefendantDetailUpdates(envelope));
    }

    @Handles("sjp.query.transparency-report-metadata")
    public JsonEnvelope getTransparencyReportMetadata(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, TRANSPARENCY_REPORT_METADATA_RESPONSE_NAME).apply(
                transparencyReportService.getMetaData());
    }

    @Handles("sjp.query.not-guilty-plea-cases")
    public JsonEnvelope getNotGuiltyPleaCases(final JsonEnvelope query) {
        final JsonObject queryFilters = query.payloadAsJsonObject();

        final String prosecutingAuthority = getString(queryFilters, "prosecutingAuthority");
        final int pageSize = queryFilters.getInt("pageSize");
        final int pageNumber = queryFilters.getInt("pageNumber");

        if(pageNumber <= 0 || pageSize <= 0) {
            throw new IllegalArgumentException(format("invalid page number (%d) or page size (%d)", pageNumber, pageSize));
        }

        final JsonObject result = caseService.buildNotGuiltyPleaCasesView(prosecutingAuthority, pageSize, pageNumber);

        return enveloper.withMetadataFrom(query, NOT_GUILTY_PLEA_CASES_RESPONSE_NAME).apply(result);
    }

    private static String extract(JsonEnvelope envelope, String fieldName) {
        return envelope.payloadAsJsonObject().getString(fieldName);
    }
}
