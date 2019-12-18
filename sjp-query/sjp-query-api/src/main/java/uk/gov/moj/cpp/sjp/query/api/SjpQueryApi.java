package uk.gov.moj.cpp.sjp.query.api;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.api.decorator.DecisionDecorator;
import uk.gov.moj.cpp.sjp.query.api.decorator.DocumentMetadataDecorator;
import uk.gov.moj.cpp.sjp.query.api.decorator.OffenceDecorator;
import uk.gov.moj.cpp.sjp.query.api.helper.SjpQueryHelper;
import uk.gov.moj.cpp.sjp.query.api.service.SjpVerdictService;
import uk.gov.moj.cpp.sjp.query.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import javax.inject.Inject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.QUERY_API)
public class SjpQueryApi {

    @Inject
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    @Inject
    private DocumentMetadataDecorator documentMetadataDecorator;

    @Inject
    private OffenceDecorator offenceDecorator;

    @Inject
    private DecisionDecorator decisionDecorator;

    @Inject
    private ReferenceDataService referenceDataService;

    @Inject
    private SjpVerdictService verdictService;

    @Handles("sjp.query.case")
    public JsonEnvelope findCase(final JsonEnvelope query) {
        final JsonEnvelope caseResponse = requester.request(query);

        if (JsonValue.NULL.equals(caseResponse.payload())) {
            return caseResponse;
        } else {
            final WithdrawalReasons withdrawalReasons = new WithdrawalReasons(referenceDataService, query);
            return enveloper.withMetadataFrom(caseResponse, caseResponse.metadata().name())
                    .apply(decisionDecorator.decorate(
                            offenceDecorator.decorateAllOffences(caseResponse.payloadAsJsonObject(), query, withdrawalReasons), query, withdrawalReasons));
        }
    }

    @Handles("sjp.query.case-with-document-metadata")
    public JsonEnvelope findCaseWithDocumentMetadata(final JsonEnvelope query) {
        final JsonEnvelope requestCaseEnvelope = enveloper.withMetadataFrom(query, "sjp.query.case")
                .apply(query.asJsonObject());

        final JsonEnvelope aCase = findCase(requestCaseEnvelope);
        return enveloper.withMetadataFrom(query, "sjp.query.case-with-document-metadata")
                .apply(documentMetadataDecorator.decorateDocumentsForACase(aCase.payloadAsJsonObject(), query));
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

    @Handles("sjp.query.all-financial-means")
    public JsonEnvelope queryAllFinancialMeans(final JsonEnvelope sourceQueryEnvelope) {

        final JsonEnvelope financialMeansEnvelope = this.findFinancialMeans(enveloper
                .withMetadataFrom(sourceQueryEnvelope, "sjp.query.financial-means")
                .apply(sourceQueryEnvelope.payload()));
        final String employmentStatus = financialMeansEnvelope.payloadAsJsonObject().getString("employmentStatus", null);

        final JsonObjectBuilder allFinancialMeans = createObjectBuilder();
        ofNullable(employmentStatus)
                .filter(e -> "EMPLOYED".equals(employmentStatus))
                .map(e -> this.findEmployer(enveloper
                        .withMetadataFrom(sourceQueryEnvelope, "sjp.query.employer")
                        .apply(sourceQueryEnvelope.payload())).payload())
                .ifPresent(e -> allFinancialMeans.add("employer", e));

        ofNullable(financialMeansEnvelope.asJsonObject().get("employmentStatus"))
                .map(e -> {
                    final JsonObjectBuilder employmentBuilder = createObjectBuilder();
                    employmentBuilder.add("status", SjpQueryHelper.INSTANCE.resolveEffectiveStatus(employmentStatus));
                    SjpQueryHelper.INSTANCE.resolveDetail(employmentStatus).ifPresent(d -> employmentBuilder.add("details", e));
                    return employmentBuilder.build();
                })
                .ifPresent(e -> allFinancialMeans.add("employment", e));

        ofNullable(financialMeansEnvelope.payloadAsJsonObject().get("defendantId")).ifPresent(e -> allFinancialMeans.add("defendantId", e));
        ofNullable(financialMeansEnvelope.payloadAsJsonObject().get("income")).ifPresent(e -> allFinancialMeans.add("income", e));
        ofNullable(financialMeansEnvelope.payloadAsJsonObject().get("benefits")).ifPresent(e -> allFinancialMeans.add("benefits", e));

        return enveloper
                .withMetadataFrom(sourceQueryEnvelope, "sjp.query.all-financial-means")
                .apply(allFinancialMeans.build());
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

    //TODO CRC-3502 - Tech debt has been created to change the output format.
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

    @Handles("sjp.query.offence-verdicts")
    public JsonEnvelope getOffencesVerdicts(final JsonEnvelope queryEnvelope) {
        return envelopeFrom(
                metadataFrom(queryEnvelope.metadata()).
                        withName("sjp.query.offence-verdicts"),
                verdictService.calculateVerdicts(queryEnvelope.payloadAsJsonObject()));
    }

    @Handles("sjp.query.case-results")
    public JsonEnvelope getCaseResults(final JsonEnvelope query) {
        return requester.request(query);
    }
}
