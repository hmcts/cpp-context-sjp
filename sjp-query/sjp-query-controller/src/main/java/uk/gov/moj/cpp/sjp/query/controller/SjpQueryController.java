package uk.gov.moj.cpp.sjp.query.controller;

import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.controller.service.PeopleService;
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
    private PeopleService peopleService;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.query.case")
    public JsonEnvelope findCase(final JsonEnvelope query) {
        if (userAndGroupsService.isSjpProsecutor(query)) {
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

    @Handles("sjp.query.sjp-case-by-urn")
    public JsonEnvelope findSjpCaseByUrn(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-by-urn-postcode")
    public JsonEnvelope findCaseByUrnPostcode(final JsonEnvelope query) {

        final JsonObject payload = query.payloadAsJsonObject();
        final String urn = payload.getString("urn");
        final String postcode = payload.getString("postcode");

        // Find the case by urn
        final JsonValue caseDetails = requester.request(enveloper.withMetadataFrom(query,
                "sjp.query.case-by-urn").apply(createObjectBuilder()
                .add("urn", urn).build())).payload();

        // Find the person and check that the postcode matches
        final JsonValue responsePayload = JsonValue.NULL.equals(caseDetails) ? null :
                peopleService.addPersonInfoForDefendantWithMatchingPostcode(postcode, (JsonObject) caseDetails, query);

        return enveloper.withMetadataFrom(query, "sjp.query.case-by-urn-response")
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

    @Handles("sjp.query.cases-search")
    public JsonEnvelope searchCasesByPersonId(final JsonEnvelope query) {
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
        if (userAndGroupsService.isSjpProsecutor(query)) {
            final JsonObject payload = createObjectBuilder()
                    .add(CASE_ID, query.asJsonObject().getString(CASE_ID))
                    .build();
            return requester.request(enveloper.withMetadataFrom(query, "sjp.query.case-documents-filter-other-and-financial-means")
                    .apply(payload));
        } else {
            return requester.request(query);
        }
    }

    @Handles("sjp.query.case-defendants")
    public JsonEnvelope findCaseDefendants(final JsonEnvelope query) {
        return requester.request(query);
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

}
