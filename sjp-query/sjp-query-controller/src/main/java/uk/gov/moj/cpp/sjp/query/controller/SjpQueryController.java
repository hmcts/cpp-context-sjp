package uk.gov.moj.cpp.sjp.query.controller;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.controller.converter.CaseConverter;
import uk.gov.moj.cpp.sjp.query.controller.service.UserAndGroupsService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.QUERY_CONTROLLER)
public class SjpQueryController {

    private static final String CASE_ID = "caseId";

    @Inject
    private Requester requester;

    @Inject
    private UserAndGroupsService userAndGroupsService;

    @Inject
    private CaseConverter caseConverter;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.query.case")
    public JsonEnvelope findCase(final JsonEnvelope query) {
        if (userAndGroupsService.isSjpProsecutorUserGroupOnly(query)) {
            final JsonObject payload = createObjectBuilder()
                    .add(CASE_ID, query.asJsonObject().getString(CASE_ID))
                    .build();
            return requester.request(enveloper.withMetadataFrom(query, "sjp.query.case-filter-other-and-financial-means-documents")
                    .apply(payload));
        } else {
            return requester.request(query);
        }
    }

    @Handles("sjp.query.case-by-urn")
    public JsonEnvelope findCaseByUrn(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-by-urn-postcode")
    public JsonEnvelope findCaseByUrnPostcode(final JsonEnvelope query) {

        final JsonValue caseDetails = requester.request(query).payload();

        final JsonValue responsePayload = !JsonValue.NULL.equals(caseDetails) ?
                caseConverter.addOffenceReferenceDataToOffences((JsonObject) caseDetails, query) : null;

        return enveloper.withMetadataFrom(query, "sjp.query.case-by-urn-postcode")
                .apply(responsePayload);
    }

    @Handles("sjp.query.financial-means")
    public JsonEnvelope findFinancialMeans(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.employer")
    public JsonEnvelope findEmployer(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-search-results")
    public JsonEnvelope findCaseSearchResults(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.cases-missing-sjpn")
    public JsonEnvelope findCasesMissingSjpn(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.cases-missing-sjpn-with-details")
    public JsonEnvelope findCasesMissingSjpnWithDetails(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-documents")
    public JsonEnvelope findCaseDocuments(final JsonEnvelope query) {
        if (userAndGroupsService.isSjpProsecutorUserGroupOnly(query)) {
            final JsonObject payload = createObjectBuilder()
                    .add(CASE_ID, query.asJsonObject().getString(CASE_ID))
                    .build();
            return requester.request(enveloper.withMetadataFrom(query, "sjp.query.case-documents-filter-other-and-financial-means")
                    .apply(payload));
        } else {
            return requester.request(query);
        }
    }

    @Handles("sjp.query.cases-search-by-material-id")
    public JsonEnvelope searchCaseByMaterialId(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.awaiting-cases")
    public JsonEnvelope getAwaitingCases(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.cases-referred-to-court")
    public JsonEnvelope getCasesReferredToCourt(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.not-ready-cases-grouped-by-age")
    public JsonEnvelope getNotReadyCasesGroupedByAge(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.oldest-case-age")
    public JsonEnvelope getOldestCaseAge(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.result-orders")
    public JsonEnvelope getResultOrders(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.defendants-online-plea")
    public JsonEnvelope getDefendantsOnlinePlea(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.pending-dates-to-avoid")
    public JsonEnvelope getPendingDatesToAvoid(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.ready-cases-reasons-counts")
    public JsonEnvelope getReadyCasesReasonsCounts(final JsonEnvelope query) {
        return requester.request(query);
    }

}
