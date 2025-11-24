package uk.gov.moj.cpp.sjp.query.api;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.query.api.converter.CaseConverter;
import uk.gov.moj.cpp.sjp.query.api.decorator.DecisionDecorator;
import uk.gov.moj.cpp.sjp.query.api.decorator.DocumentMetadataDecorator;
import uk.gov.moj.cpp.sjp.query.api.decorator.OffenceDecorator;
import uk.gov.moj.cpp.sjp.query.api.helper.SjpQueryHelper;
import uk.gov.moj.cpp.sjp.query.api.service.SjpVerdictService;
import uk.gov.moj.cpp.sjp.query.api.validator.SjpQueryApiValidator;
import uk.gov.moj.cpp.sjp.query.service.OffenceFineLevels;
import uk.gov.moj.cpp.sjp.query.service.ReferenceDataService;
import uk.gov.moj.cpp.sjp.query.service.WithdrawalReasons;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.QUERY_API)
public class SjpQueryApi {

    public static final String DEFENDANT_ID = "defendantId";
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

    @Inject
    private SjpQueryApiValidator sjpQueryApiValidator;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private CaseConverter caseConverter;

    private static final Logger LOGGER = LoggerFactory.getLogger(SjpQueryApi.class);

    @Handles("sjp.query.case")
    public JsonEnvelope findCase(final JsonEnvelope query) {
        final JsonEnvelope caseResponse = requester.request(query);

        if (JsonValue.NULL.equals(caseResponse.payload())) {
            return caseResponse;
        } else {
            final WithdrawalReasons withdrawalReasons = new WithdrawalReasons(referenceDataService, query);
            final OffenceFineLevels offenceFineLevels = new OffenceFineLevels(referenceDataService, query);

            return enveloper.withMetadataFrom(caseResponse, caseResponse.metadata().name())
                    .apply(decisionDecorator.decorate(
                            offenceDecorator.decorateAllOffences(caseResponse.payloadAsJsonObject(), query, withdrawalReasons, offenceFineLevels), query, withdrawalReasons));
        }
    }

    @Handles("sjp.query.prosecution-case")
    public JsonEnvelope findProsecutionCase(final JsonEnvelope query) {
        final JsonEnvelope caseResponse = requester.request(query);

        if (JsonValue.NULL.equals(caseResponse.payload())) {
            return caseResponse;
        } else {
            return envelopeFrom(metadataFrom(caseResponse.metadata()), caseResponse.payloadAsJsonObject());
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

    @Handles("sjp.query.defendant-potential-cases")
    public JsonEnvelope findDefendantPotentialCases(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.case-by-urn-postcode")
    public JsonEnvelope findCaseByUrnPostcode(final JsonEnvelope query) {
        final JsonValue caseDetails = requester.request(query).payload();
        if (null != caseDetails && caseDetails.getValueType() == JsonValue.ValueType.NULL) {
            throw new NotFoundException();
        }
        final Map<String, List<String>> validationErrors = sjpQueryApiValidator.validateCasePostConviction((JsonObject)caseDetails);

        if (!validationErrors.isEmpty()) {
            throw new BadRequestException(objectToJsonValueConverter.convert(validationErrors).toString());
        }

        final JsonValue responsePayload = !JsonValue.NULL.equals(caseDetails) ?
                caseConverter.addOffenceReferenceDataToOffences((JsonObject) caseDetails, query) : null;

        return enveloper.withMetadataFrom(query, "sjp.query.case-by-urn-postcode")
                .apply(responsePayload);
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

        ofNullable(financialMeansEnvelope.payloadAsJsonObject().get(DEFENDANT_ID)).ifPresent(e -> allFinancialMeans.add(DEFENDANT_ID, e));
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

    @Handles("sjp.query.press-transparency-report-metadata")
    public JsonEnvelope getPressTransparencyReportMetadata(final JsonEnvelope query) {
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

    @Handles("sjp.query.common-case-application")
    public JsonEnvelope getCommonCaseApplication(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.not-guilty-plea-cases")
    public JsonEnvelope getNotGuiltyPleaCases(final JsonEnvelope query) {
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
                    .add(DEFENDANT_ID, defendantProfilingView.getId().toString())
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

    private void logSummary(final DefendantProfilingView defendantProfilingView, final JsonObject outstandingFines) {
        int numberOfFines = 0;
        try {
            numberOfFines = outstandingFines.getJsonArray("outstandingFines").size();
        } catch (final ClassCastException e) {
            LOGGER.trace(e.getMessage(), e);
        }
        LOGGER.info("OutstandingFines for {}: {} ", defendantProfilingView.getId(), numberOfFines);
    }

    @Handles("sjp.query.cases-without-defendant-postcode")
    public JsonEnvelope getCasesWithoutDefendantPostcode(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.account-note")
    public JsonEnvelope getAccountNotes(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.cases-for-soc-check")
    public JsonEnvelope getCasesForSOCCheck(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.prosecuting-authority-for-lja")
    public JsonEnvelope getProsecutingAuthorityForLja(final JsonEnvelope query) {
        return requester.request(query);
    }

}