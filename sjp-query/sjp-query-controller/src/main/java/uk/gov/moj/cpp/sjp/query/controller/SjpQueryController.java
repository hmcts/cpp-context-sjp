package uk.gov.moj.cpp.sjp.query.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.controller.response.DefendantProfilingView;
import uk.gov.moj.cpp.sjp.query.controller.service.UserAndGroupsService;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.Objects;

import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.QUERY_CONTROLLER)
public class SjpQueryController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SjpQueryController.class);

    private static final String CASE_ID = "caseId";

    @Inject
    private Requester requester;

    @Inject
    private UserAndGroupsService userAndGroupsService;

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

    @Handles("sjp.query.prosecution-case")
    public JsonEnvelope findProsecutionCase(final JsonEnvelope query) {
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

    @Handles("sjp.query.ready-cases")
    public JsonEnvelope getReadyCases(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-assignment")
    public JsonEnvelope getCaseAssignment(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-prosecuting-authority")
    public JsonEnvelope getProsecutingAuthority(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.defendant-details-updates")
    public JsonEnvelope getDefendantDetailsUpdates(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-notes")
    public JsonEnvelope getCaseNotes(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.transparency-report-metadata")
    public JsonEnvelope getTransparencyReportMetadata(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.press-transparency-report-metadata")
    public JsonEnvelope getPressTransparencyReportMetadata(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-court-extract")
    public JsonEnvelope getCourtExtract(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-results")
    public JsonEnvelope getCaseResults(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.common-case-application")
    public JsonEnvelope getCommonCaseApplication(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.not-guilty-plea-cases")
    public JsonEnvelope getNotGuiltyPleaCases(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.cases-without-defendant-postcode")
    public JsonEnvelope getCasesWithoutDefendantPostcode(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.defendant-outstanding-fines")
    public JsonEnvelope getOutstandingFines(final JsonEnvelope query) {

        final Envelope<DefendantProfilingView> profilingViewEnvelope = requester.request(
                enveloper.withMetadataFrom(query, "sjp.query.defendant-profile")
                        .apply(query.payloadAsJsonObject()), DefendantProfilingView.class);


        final DefendantProfilingView defendantProfilingView = profilingViewEnvelope.payload();
        JsonObject payload = null;
        if (defendantProfilingView != null) {
            final JsonObjectBuilder payloadBuilder = createObjectBuilder();
            payloadBuilder
                    .add("defendantId",defendantProfilingView.getId().toString())
                    .add("firstname", defendantProfilingView.getFirstName())
                    .add("lastname", defendantProfilingView.getLastName());
            if (!Objects.isNull(defendantProfilingView.getDateOfBirth())) {
                payloadBuilder.add("dob", defendantProfilingView.getDateOfBirth().toString());
            }
            if (!Objects.isNull(defendantProfilingView.getNationalInsuranceNumber())) {
                payloadBuilder.add("ninumber", defendantProfilingView.getNationalInsuranceNumber());
            }
            payload = payloadBuilder.build();
            final JsonEnvelope enforcementRequestEnvelope = requester.requestAsAdmin(
                    enveloper.withMetadataFrom(query, "stagingenforcement.defendant.outstanding-fines")
                            .apply(payload));

            final JsonObject outstandingFines = enforcementRequestEnvelope.payloadAsJsonObject();

            logSummary(defendantProfilingView, outstandingFines);

            return enveloper.withMetadataFrom(query, "sjp.query.defendant-outstanding-fines")
                    .apply(outstandingFines);
        } else {
            return enveloper.withMetadataFrom(query, "sjp.query.defendant-outstanding-fines")
                    .apply(createObjectBuilder().add("outstandingFines", createArrayBuilder()).build());
        }
    }

    @Handles("sjp.query.account-note")
    public JsonEnvelope getAccountNotes(final JsonEnvelope query) {
        return requester.request(query);
    }

    private void logSummary(final DefendantProfilingView defendantProfilingView, final JsonObject outstandingFines) {
        int numberOfFines = 0;
        try {
            numberOfFines = outstandingFines.getJsonArray("outstandingFines").size();
        } catch (final ClassCastException e) {
            LOGGER.trace(e.getMessage(), e);
        }
        LOGGER.info("OutstandingFines for {}: {} ", defendantProfilingView.getId(), numberOfFines);
    }
}
