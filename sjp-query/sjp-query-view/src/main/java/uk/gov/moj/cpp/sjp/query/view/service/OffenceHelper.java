package uk.gov.moj.cpp.sjp.query.view.service;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static java.util.Optional.of;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.moj.cpp.sjp.query.view.converter.ResultCode.RSJP;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.VERDICT;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.Employer;
import uk.gov.moj.cpp.sjp.query.view.converter.ResultCode;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.slf4j.Logger;

public class OffenceHelper {

    private static final Logger LOGGER = getLogger(OffenceHelper.class);
    private static final String OFFENCES = "offences";
    private static final String OFFENCE_ID = "id";
    private static final String RESULTS = "results";
    private static final String CODE = "code";
    private static final String RESULT_DEFINITION_ID = "resultDefinitionId";
    private static final String PROMPTS = "prompts";
    private static final String ID = "id";

    private ReferenceDataService referenceDataService;

    @Inject
    public OffenceHelper(final ReferenceDataService referenceDataService) {
        this.referenceDataService = referenceDataService;
    }

    public JsonArray populateOffences(final CaseView caseView, final Employer employer, final JsonEnvelope sourceEnvelope){
        final JsonArrayBuilder offencesPayloadBuilder = createArrayBuilder();
        final JsonObject decision = sourceEnvelope.payloadAsJsonObject();
        if(decision.containsKey(OFFENCES)){
            final OffenceDataSupplier offenceDataSupplier = OffenceDataSupplier.create(sourceEnvelope, caseView, employer, referenceDataService);
            decision.getJsonArray(OFFENCES)
                    .getValuesAs(JsonObject.class)
                    .forEach(jsonOffence -> createOffence(
                            offencesPayloadBuilder,
                            jsonOffence,
                            offenceDataSupplier));
        }
        return offencesPayloadBuilder.build();
    }

    private void createOffence(final JsonArrayBuilder offencesPayloadBuilder, final JsonObject jsonOffence, final OffenceDataSupplier offenceDataSupplier){

        final JsonObjectBuilder offencePayloadBuilder = createObjectBuilder();
        final String offenceId = jsonOffence.getString(ID, null);
        final String verdict = jsonOffence.getString(VERDICT, null);

        if(nonNull(offenceId)) {
            offencePayloadBuilder.add(OFFENCE_ID, offenceId);
        }
        if(nonNull(verdict)) {
            offencePayloadBuilder.add(VERDICT, verdict);
        }
        final Optional<JsonArrayBuilder> resultsPayloadBuilderOpt = createResults(jsonOffence, offenceDataSupplier);
        resultsPayloadBuilderOpt.ifPresent(resultsPayloadBuilder -> offencePayloadBuilder.add(RESULTS, resultsPayloadBuilder));
        offencesPayloadBuilder.add(offencePayloadBuilder);
    }

    private Optional<JsonArrayBuilder> createResults(final JsonObject jsonOffence, final OffenceDataSupplier offenceDataSupplier){
        if(jsonOffence.containsKey(RESULTS)) {
            final JsonArrayBuilder resultsPayloadBuilder = createArrayBuilder();
            jsonOffence.getJsonArray(RESULTS).getValuesAs(JsonObject.class).forEach(result -> createResult(resultsPayloadBuilder, result, offenceDataSupplier));
            return of(resultsPayloadBuilder);
        }
        return Optional.empty();
    }

    private void createResult(final JsonArrayBuilder resultsPayloadBuilder, final JsonObject result, final OffenceDataSupplier offenceDataSupplier){
        final Optional<ResultCode> resultCode = parseCode(result);
        if(resultCode.isPresent()) {
            final JsonObjectBuilder resultPayloadBuilder = createObjectBuilder();
            resultPayloadBuilder.add(RESULT_DEFINITION_ID, (resultCode.get().equals(RSJP)) ? ResultCode.SJPR.getResultDefinitionId().toString() : resultCode.get().getResultDefinitionId().toString());
            resultPayloadBuilder.add(PROMPTS, createPrompts(result, resultCode.get(), offenceDataSupplier));
            resultsPayloadBuilder.add(resultPayloadBuilder);
        } else if(LOGGER.isWarnEnabled()){
            LOGGER.warn(format("Ignoring result code %s , is not mapped.", result.getString(CODE, null)));
        }
    }

    private JsonArrayBuilder createPrompts(final JsonObject result, final ResultCode resultCode, final OffenceDataSupplier offenceDataSupplier){
        return resultCode.createPrompts(result, offenceDataSupplier);
    }

    private Optional<ResultCode> parseCode(final JsonObject result){
        final String code = result.getString(CODE, null);
        return ResultCode.parse("ADJOURN".equalsIgnoreCase(code) ? "ADJOURNSJP" : code);
    }
}
