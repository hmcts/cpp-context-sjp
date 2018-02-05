package uk.gov.moj.cpp.sjp.query.controller;

import static javax.json.Json.createObjectBuilder;
import static org.apache.commons.lang3.StringUtils.deleteWhitespace;

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

        // Check that postcode matches.

        final JsonValue responsePayload;
        if (JsonValue.NULL.equals(caseDetails)) {
            responsePayload = null;
        }
        else {
            final JsonObject caseJsonObject = (JsonObject) caseDetails;
            final JsonObject address = caseJsonObject.getJsonObject("defendant")
                    .getJsonObject("personalDetails")
                    .getJsonObject("address");

            if (deleteWhitespace(postcode).equals(deleteWhitespace(address.getString("postcode")))) {
                responsePayload = caseConverter.addOffenceReferenceDataToOffences((JsonObject) caseDetails, query);
            }
            else {
                responsePayload = null;
            }
        }

        //Payload json with json path "$['defendant']['personalDetails']['firstName']" evaluated to "firstName" and Payload json with json path "$['defendant']['personalDetails']['lastName']" evaluated to "lastName" and Payload json with json path "$['defendant']['personalDetails']['dateOfBirth']" evaluated to "1980-07-15" and Payload json with json path "$['defendant']['personalDetails']['home']" evaluated to "02012345678" and Payload json with json path "$['defendant']['personalDetails']['mobile']" evaluated to "07777888999" and Payload json with json path "$['defendant']['personalDetails']['email']" evaluated to "email@email.com" and Payload json with json path "$['defendant']['personalDetails']['nationalInsuranceNumber']" evaluated to "AA123456C" and Payload json with json path "$['defendant']['personalDetails']['address']['address1']" evaluated to "address1" and Payload json with json path "$['defendant']['personalDetails']['address']['address2']" evaluated to "address2" and Payload json with json path "$['defendant']['personalDetails']['address']['address3']" evaluated to "address3" and Payload json with json path "$['defendant']['personalDetails']['address']['address4']" evaluated to "address4" and Payload json with json path "$['defendant']['personalDetails']['address']['postcode']" evaluated to "W1T 1JY" and Payload json with json path "$['defendant']['offences'][0]['title']" evaluated to "Public service vehicle - passenger use altered / defaced   ticket" and Payload json with json path "$['defendant']['offences'][0]['wording']" evaluated to "Committed some offence" and Payload json with json path "$['defendant']['offences'][0]['pendingWithdrawal']" evaluated to <false>)
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

    @Handles("sjp.query.defendants-online-plea")
    public JsonEnvelope getDefendantsOnlinePlea(final JsonEnvelope query) {
        return requester.request(query);
    }
}
