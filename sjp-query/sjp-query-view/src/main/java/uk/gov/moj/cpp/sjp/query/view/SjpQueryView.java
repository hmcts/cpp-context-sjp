package uk.gov.moj.cpp.sjp.query.view;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toSet;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.domain.FinancialMeans;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;
import uk.gov.moj.cpp.sjp.persistence.repository.OnlinePleaRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.PendingDatesToAvoidRepository;
import uk.gov.moj.cpp.sjp.query.view.response.DatesToAvoidsView;
import uk.gov.moj.cpp.sjp.query.view.service.AssignmentService;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;
import uk.gov.moj.cpp.sjp.query.view.service.EmployerService;
import uk.gov.moj.cpp.sjp.query.view.service.FinancialMeansService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
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
    private static final String NAME_RESPONSE_CASE_DEFENDANTS = "sjp.query.case-defendants-response";
    private static final String NAME_RESPONSE_AWAITING_CASES = "sjp.query.awaiting-cases-response";
    private static final String NAME_RESPONSE_CASES_REFERRED_TO_COURT = "sjp.query.cases-referred-to-court-response";
    private static final String NAME_RESPONSE_CASES_WITH_ORDER = "sjp.query.result-orders";

    @Inject
    private CaseService caseService;

    @Inject
    private AssignmentService assignmentService;

    @Inject
    private FinancialMeansService financialMeansService;

    @Inject
    private EmployerService employerService;

    @Inject
    private OnlinePleaRepository.FinancialMeansOnlinePleaRepository onlinePleaRepository;

    @Inject
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.query.case")
    public JsonEnvelope findCase(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE).apply(
                caseService.findCase(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID)));
    }

    @Handles("sjp.query.case-filter-other-and-financial-means-documents")
    public JsonEnvelope findCaseAndFilterOtherAndFinancialMeansDocuments(JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE)
                .apply(caseService.findCaseAndFilterOtherAndFinancialMeansDocuments(
                        envelope.payloadAsJsonObject().getString(FIELD_CASE_ID)));
    }


    @Handles("sjp.query.case-by-urn")
    public JsonEnvelope findCaseByUrn(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE).apply(
                caseService.findCaseByUrn(envelope.payloadAsJsonObject().getString(FIELD_URN)));
    }

    @Handles("sjp.query.case-by-urn-postcode")
    public JsonEnvelope findCaseByUrnPostcode(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE).apply(
                caseService.findCaseByUrnPostcode(envelope.payloadAsJsonObject().getString(FIELD_URN),
                        envelope.payloadAsJsonObject().getString(FIELD_POSTCODE)));
    }

    @Handles("sjp.query.cases-search")
    public JsonEnvelope searchCasesByPersonId(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASES_SEARCH).apply(
                caseService.searchCasesByDefendantId(envelope.payloadAsJsonObject().getString(FIELD_QUERY)));
    }

    @Handles("sjp.query.case-search-results")
    public JsonEnvelope findCaseSearchResults(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASES_SEARCH).apply(
                caseService.searchCases(envelope, envelope.payloadAsJsonObject().getString(FIELD_QUERY)));
    }

    @Handles("sjp.query.cases-missing-sjpn")
    public JsonEnvelope findCasesMissingSjpn(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final Optional<Integer> limit = payload.containsKey("limit") ? Optional.of(payload.getInt("limit")) : empty();
        final Optional<LocalDate> postedBefore = payload.containsKey(FIELD_DAYS_SINCE_POSTING) ?
                Optional.of(LocalDate.now().minusDays(payload.getInt(FIELD_DAYS_SINCE_POSTING))) : empty();

        return enveloper.withMetadataFrom(envelope, "sjp.query.cases-missing-sjpn")
                .apply(caseService.findCasesMissingSjpn(limit, postedBefore));
    }

    @Handles("sjp.query.cases-missing-sjpn-with-details")
    public JsonEnvelope findCasesMissingSjpnWithDetails(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final Optional<LocalDate> postedBefore = payload.containsKey(FIELD_DAYS_SINCE_POSTING) ?
                Optional.of(LocalDate.now().minusDays(payload.getInt(FIELD_DAYS_SINCE_POSTING))) : empty();
        return enveloper.withMetadataFrom(envelope, "sjp.query.cases-missing-sjpn-with-details")
                .apply(caseService.findCasesMissingSjpnWithDetails(postedBefore));
    }

    @Handles("sjp.query.case-documents")
    public JsonEnvelope findCaseDocuments(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE_DOCUMENTS).apply(
                caseService.findCaseDocuments(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID)));
    }

    @Handles("sjp.query.case-documents-filter-other-and-financial-means")
    public JsonEnvelope findCaseDocumentsFilterOtherAndFinancialMeans(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE_DOCUMENTS).apply(
                caseService.findCaseDocumentsFilterOtherAndFinancialMeans(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID)));

    }

    @Handles("sjp.query.case-defendants")
    public JsonEnvelope findCaseDefendants(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASE_DEFENDANTS)
                .apply(caseService.findCaseDefendants(
                        envelope.payloadAsJsonObject().getString(FIELD_CASE_ID)));
    }

    @Handles("sjp.query.financial-means")
    public JsonEnvelope findFinancialMeans(final JsonEnvelope envelope) {
        final UUID defendantId = UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_DEFENDANT_ID));
        final Optional<FinancialMeans> financialMeans = financialMeansService.getFinancialMeans(defendantId);
        return enveloper.withMetadataFrom(envelope, "sjp.query.financial-means")
                .apply(financialMeans.orElse(new FinancialMeans(null, null, null, null)));
    }

    @Handles("sjp.query.employer")
    public JsonEnvelope findEmployer(final JsonEnvelope envelope) {
        final UUID defendantId = UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_DEFENDANT_ID));
        final Optional<Employer> employer = employerService.getEmployer(defendantId);
        return enveloper.withMetadataFrom(envelope, "sjp.query.employer")
                .apply(employer.orElse(new Employer(null, null, null, null, null)));
    }

    @Handles("sjp.query.cases-search-by-material-id")
    public JsonEnvelope searchCaseByMaterialId(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASES_SEARCH_BY_MATERIAL_ID).apply(
                caseService.searchCaseByMaterialId(envelope.payloadAsJsonObject().getString(FIELD_QUERY)));

    }

    @Handles("sjp.query.awaiting-cases")
    public JsonEnvelope getAwaitingCases(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_AWAITING_CASES).apply(
                caseService.findAwaitingCases());
    }

    @Handles("sjp.query.cases-referred-to-court")
    public JsonEnvelope getCasesReferredToCourt(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASES_REFERRED_TO_COURT).apply(
                caseService.findCasesReferredToCourt());
    }

    @Handles("sjp.query.not-ready-cases-grouped-by-age")
    public JsonEnvelope findNotReadyCasesGroupedByAge(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, "sjp.query.not-ready-cases-grouped-by-age")
                .apply(caseService.getNotReadyCasesGroupedByAge());
    }

    @Handles("sjp.query.oldest-case-age")
    public JsonEnvelope findOldestCaseAge(final JsonEnvelope envelope) {
        return enveloper.withMetadataFrom(envelope, "sjp.query.oldest-case-age")
                .apply(caseService.getOldestCaseAge());
    }

    @Handles(NAME_RESPONSE_CASES_WITH_ORDER)
    public JsonEnvelope getResultOrders(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        final LocalDate fromDate = LocalDates.from(payload.getString("fromDate"));
        final LocalDate toDate = LocalDates.from(payload.getString("toDate"));

        return enveloper.withMetadataFrom(envelope, NAME_RESPONSE_CASES_WITH_ORDER)
                .apply(caseService.findResultOrders(fromDate, toDate));
    }

    @Handles("sjp.query.defendants-online-plea")
    public JsonEnvelope findDefendantsOnlinePlea(final JsonEnvelope envelope) {
        final UUID caseId = UUID.fromString(envelope.payloadAsJsonObject().getString(FIELD_CASE_ID));
        return enveloper.withMetadataFrom(envelope, "sjp.query.defendants-online-plea")
                .apply(onlinePleaRepository.findBy(caseId));
    }

    @Handles("sjp.query.assignment-candidates")
    public JsonEnvelope findAssignmentCandidates(final JsonEnvelope envelope) {
        final JsonObject queryOptions = envelope.payloadAsJsonObject();

        final UUID assigneeId = UUID.fromString(queryOptions.getString("assigneeId"));
        final SessionType sessionType = SessionType.valueOf(queryOptions.getString("sessionType"));
        final int limit = queryOptions.getInt("limit");
        final String excludedProsecutingAuthoritiesAsString = queryOptions.getString("excludedProsecutingAuthorities", "");

        final Set<String> excludedProsecutingAuthorities = Stream.of(excludedProsecutingAuthoritiesAsString.split(","))
                .map(String::trim)
                .filter(prosecutor -> !prosecutor.isEmpty())
                .collect(toSet());

        final List<AssignmentCandidate> assignmentCandidatesList = assignmentService.getAssignmentCandidates(assigneeId, sessionType, excludedProsecutingAuthorities, limit);

        final JsonArrayBuilder casesReadyForDecisionBuilder = Json.createArrayBuilder();

        assignmentCandidatesList.forEach(assignmentCandidate -> casesReadyForDecisionBuilder.add(createObjectBuilder()
                .add("caseId", assignmentCandidate.getCaseId().toString())
                .add("caseStreamVersion", assignmentCandidate.getCaseStreamVersion())
        ));

        final JsonObject casesReadyForDecision = createObjectBuilder()
                .add("assignmentCandidates", casesReadyForDecisionBuilder.build())
                .build();

        return enveloper.withMetadataFrom(envelope, "sjp.query.assignment-candidates").apply(casesReadyForDecision);
    }

    @Handles("sjp.query.pending-dates-to-avoid")
    public JsonEnvelope findPendingDatesToAvoid(final JsonEnvelope envelope) {
        final List<PendingDatesToAvoid> pendingDatesToAvoidList = pendingDatesToAvoidRepository.findCasesPendingDatesToAvoid();
        return enveloper.withMetadataFrom(envelope, "sjp.pending-dates-to-avoid")
                .apply(new DatesToAvoidsView(pendingDatesToAvoidList));
    }

}
