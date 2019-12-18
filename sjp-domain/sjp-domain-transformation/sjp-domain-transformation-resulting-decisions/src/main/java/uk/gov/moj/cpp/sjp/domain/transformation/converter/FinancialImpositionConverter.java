package uk.gov.moj.cpp.sjp.domain.transformation.converter;


import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.util.Optional;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.ResultCodeHandler.*;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.*;

public class FinancialImpositionConverter implements Converter {

    public static final FinancialImpositionConverter INSTANCE = new FinancialImpositionConverter();

    @Override
    public JsonObject convert(final JsonObject caseLevelDecisionJsonObject,
                              final String pleaType,
                              final String verdict) {

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();
        final JsonObject results = convertAllTheResults(caseLevelDecisionJsonObject);

        buildCostsAndSurcharge(results).ifPresent(costsAndSurcharge ->
                jsonObjectBuilder.add(COSTS_AND_SURCHARGE, costsAndSurcharge)
        );

        buildPayment(results).ifPresent(payment -> jsonObjectBuilder.add(PAYMENT, payment));


        return jsonObjectBuilder.build();
    }

    @SuppressWarnings({"squid:S1188", "squid:S3776"})
    private JsonObject convertAllTheResults(JsonObject offenceLevelJsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        offenceLevelJsonObject
                .getJsonArray(RESULTS)
                .getValuesAs(JsonObject.class)
                .forEach((eachResultObject) -> {
                    final String resultCode = eachResultObject.getString(CODE);
                    handleResult(jsonObjectBuilder, eachResultObject, resultCode);
                });

        return jsonObjectBuilder.build();
    }

    @SuppressWarnings({"squid:S1611", "squid:MethodCyclomaticComplexity"})
    private void handleResult(JsonObjectBuilder jsonObjectBuilder, JsonObject eachResultObject, String resultCode) {
        switch (resultCode) {
            case FCOST:
                handleResultCodeFCOST(eachResultObject, jsonObjectBuilder);
                break;
            case FVS:
                handleResultCodeFVS(eachResultObject, jsonObjectBuilder);
                break;
            case COLLO:
                handleResultCodeCOLLO(eachResultObject, jsonObjectBuilder);
                break;
            case NCOLLO:
                handleResultCodeNCOLLO(eachResultObject, jsonObjectBuilder);
                break;
            case ABDC:
                handleResultCodeABDC(eachResultObject, jsonObjectBuilder);
                break;
            case AEOC:
                handleResultCodeAEOC(eachResultObject, jsonObjectBuilder);
                break;
            case NOVS:
                handleResultCodeNOVS(eachResultObject, jsonObjectBuilder);
                break;
            case NCOSTS:
                handleResultCodeNCOSTS(eachResultObject, jsonObjectBuilder);
                break;
            case TFOOUT:
                handleResultCodeTFOOUT(eachResultObject, jsonObjectBuilder);
                break;
            case RLSUM:
                handleResultCodeRLSUM(eachResultObject, jsonObjectBuilder);
                break;
            case RLSUMI:
                handleResultCodeRLSUMI(eachResultObject, jsonObjectBuilder);
                break;
            case RINSTL:
                handleResultCodeRINSTL(eachResultObject, jsonObjectBuilder);
                break;
            case LSUM:
                handleResultCodeLSUM(eachResultObject, jsonObjectBuilder);
                break;
            case LSUMI:
                handleResultCodeLSUMI(eachResultObject, jsonObjectBuilder);
                break;
            case INSTL:
                handleResultCodeINSTL(eachResultObject, jsonObjectBuilder);
                break;
            default:
        }
    }

    private Optional<JsonValue> buildPayment(final JsonObject results) {
        final JsonObjectBuilder paymentObjectBuilder = createObjectBuilder();
        if (!results.keySet().isEmpty()) {
            if (results.containsKey(REASON_WHY_NOT_ATTACHED_OR_DEDUCTED)) {
                paymentObjectBuilder.add(REASON_WHY_NOT_ATTACHED_OR_DEDUCTED, results.getString(REASON_WHY_NOT_ATTACHED_OR_DEDUCTED));
            }

            if (results.containsKey(TOTAL_SUM)) {
                paymentObjectBuilder.add(TOTAL_SUM, results.get(TOTAL_SUM));
            }

            if (results.containsKey(REASON_FOR_DEDUCTING_FROM_BENEFITS)) {
                paymentObjectBuilder.add(REASON_FOR_DEDUCTING_FROM_BENEFITS, results.getString(REASON_FOR_DEDUCTING_FROM_BENEFITS));
            }

            if (results.containsKey(REASON_FOR_ATTACHING_TO_EARNINGS)) {
                paymentObjectBuilder.add(REASON_FOR_ATTACHING_TO_EARNINGS, results.getString(REASON_FOR_ATTACHING_TO_EARNINGS));
            }

            if (results.containsKey(PAYMENT_TYPE)) {
                paymentObjectBuilder.add(PAYMENT_TYPE, results.getString(PAYMENT_TYPE));
            }

            buildFineTransferredTo(results, paymentObjectBuilder);

            buildPaymentTerms(results).ifPresent(paymentTerms ->
                    paymentObjectBuilder.add(PAYMENT_TERMS, paymentTerms)
            );

            final JsonObject payment = paymentObjectBuilder.build();
            if (!payment.keySet().isEmpty()) {
                return Optional.of(payment);
            }
        }
        return Optional.empty();
    }

    private void buildFineTransferredTo(JsonObject results, JsonObjectBuilder paymentObjectBuilder) {
        if (results.containsKey(NATIONAL_COURT_NAME)) {
            final JsonObjectBuilder fineTransferredToJsonObject = createObjectBuilder()
                    .add(NATIONAL_COURT_NAME, results.getString(NATIONAL_COURT_NAME));

            if (results.containsKey(NATIONAL_COURT_CODE)) {
                fineTransferredToJsonObject.add(NATIONAL_COURT_CODE, results.getString(NATIONAL_COURT_CODE));
            }

            paymentObjectBuilder.add(FINE_TRANSFERRED_TO, fineTransferredToJsonObject.build());
        }
    }

    private Optional<JsonValue> buildPaymentTerms(final JsonObject results) {

        final JsonObjectBuilder paymentTermsBuilder = createObjectBuilder();
        if (results.containsKey(RESERVE_TERMS)) {
            paymentTermsBuilder.add(RESERVE_TERMS, results.getBoolean(RESERVE_TERMS));
        }

        // lumpSum
        if (results.containsKey(LUMP_SUM_AMOUNT)) {
            final JsonObjectBuilder lumpSumBuilder = createObjectBuilder();
            if (results.containsKey(WITHIN_DAYS)) {
                lumpSumBuilder.add(WITHIN_DAYS, results.getInt(WITHIN_DAYS));
            }

            paymentTermsBuilder.add(LUMP_SUM, lumpSumBuilder
                    .add(AMOUNT, results.get(LUMP_SUM_AMOUNT)));
        }

        // installments
        if (results.containsKey(INSTALLMENT_AMOUNT)) {
            final JsonObjectBuilder installmentBuilder = createObjectBuilder();
            if (results.containsKey(INSTALLMENT_PERIOD)) {
                installmentBuilder.add(PERIOD, results.getString(INSTALLMENT_PERIOD));
            }

            if (results.containsKey(INSTALLMENT_START_DATE)) {
                installmentBuilder.add(START_DATE, results.get(INSTALLMENT_START_DATE));
            }

            paymentTermsBuilder.add(INSTALLMENTS, installmentBuilder
                    .add(AMOUNT, results.get(INSTALLMENT_AMOUNT)));
        }


        final JsonObject paymentTerms = paymentTermsBuilder.build();
        if (!paymentTerms.keySet().isEmpty()) {
            return Optional.of(paymentTerms);
        }

        return Optional.empty();
    }

    private Optional<JsonObject> buildCostsAndSurcharge(final JsonObject results) {
        final JsonObjectBuilder costsAndSurchargeBuilder = createObjectBuilder();

        if (!results.keySet().isEmpty()) {

            if (results.containsKey(COSTS)) {
                costsAndSurchargeBuilder.add(COSTS, results.get(COSTS));
            }

            if (results.containsKey(REASON_FOR_NO_COSTS)) {
                costsAndSurchargeBuilder.add(REASON_FOR_NO_COSTS, results.getString(REASON_FOR_NO_COSTS));
            }

            buildVictimSurcharge(results, costsAndSurchargeBuilder);

            if (results.containsKey(COLLECTION_ORDER_MADE)) {
                costsAndSurchargeBuilder.add(COLLECTION_ORDER_MADE, results.getBoolean(COLLECTION_ORDER_MADE));
            }

            final JsonObject costsAndSurcharge = costsAndSurchargeBuilder.build();
            if (!costsAndSurcharge.keySet().isEmpty()) {
                return Optional.of(costsAndSurcharge);
            }
        }

        return Optional.empty();
    }

    private void buildVictimSurcharge(final JsonObject results,
                                      final JsonObjectBuilder costsAndSurchargeBuilder) {

        if (results.containsKey(VICTIM_SURCHARGE)) {
            costsAndSurchargeBuilder.add(VICTIM_SURCHARGE, results.get(VICTIM_SURCHARGE));
        }

        if (results.containsKey(VICTIM_SURCHARGE_REASON_TYPE) &&
                results.containsKey(VICTIM_SURCHARGE_REASON)) {
            if (NOT_IMPOSED.equals(results.getString(VICTIM_SURCHARGE_REASON_TYPE))) {
                costsAndSurchargeBuilder.add(REASON_FOR_NO_VICTIM_SURCHARGE, results.getString(VICTIM_SURCHARGE_REASON));
            } else if (REDUCED.equals(results.getString(VICTIM_SURCHARGE_REASON_TYPE))) {
                costsAndSurchargeBuilder.add(REASON_FOR_REDUCED_VICTIM_SURCHARGE, results.getString(VICTIM_SURCHARGE_REASON));
            }
        }
    }

}
