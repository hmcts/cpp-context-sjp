package uk.gov.moj.cpp.sjp.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.QUERY_API)
public class SjpQueryApi {

    @Inject
    private Requester requester;

    @Handles("sjp.query.case")
    public JsonEnvelope findCase(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-by-urn")
    public JsonEnvelope findCaseByUrn(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-by-urn-postcode")
    public JsonEnvelope findCaseByUrnPostcode(final JsonEnvelope query) {
        return requester.request(query);
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
        return requester.request(query);
    }

    //TODO CRC-3502 - Tech debt has been created to change the output format.
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
}
