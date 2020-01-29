package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.*;

import uk.gov.moj.cpp.sjp.domain.transformation.exception.TransformationException;

@SuppressWarnings("squid:S1611")
public class ResultCodeHandler {

    public static final ResultCodeHandler resultCodeHandler = new ResultCodeHandler();

    static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy");

    private ResultCodeHandler() {
    }

    // -1 index -- not needed
    public static void handleResultCodeFO(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(FINE, new BigDecimal(getValueAsString(resultJsonObject, 1)
                .orElseThrow(() -> new TransformationException("Fine cannot be blank"))));
    }

    public static void handleResultCodeFCOMP(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(COMPENSATION, new BigDecimal(getValueAsString(resultJsonObject, 1)
                .orElseThrow(() -> new TransformationException("Compensation cannot be blank"))));
    }

    public static void handleResultCodeFCOST(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(COSTS, new BigDecimal(getValueAsString(resultJsonObject, 1)
                .orElseThrow(() -> new TransformationException("Costs cannot be blank"))));
    }

    public static void handleResultCodeFVS(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(VICTIM_SURCHARGE, new BigDecimal(getValueAsString(resultJsonObject, 1)
                .orElseThrow(() -> new TransformationException("VictimSurcharge cannot be blank"))));
    }

    // not required
    public static void handleResultCodeFVEBD(final JsonObject resultJsonObject,
                                             final JsonObjectBuilder jsonObjectBuilder) {
        throw new UnsupportedOperationException();
    }

    public static void handleResultCodeGPTAC(final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(GUILTY_PLEA_TAKEN_INTO_ACCOUNT, true);
    }

    public static void handleResultCodeAD(final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(DISCHARGE_TYPE, ABSOLUTE);
    }

    public static void handleResultCodeCD(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(DISCHARGE_TYPE, CONDITIONAL);
        final JsonObject dischargedForObject = buildDischargedFor(resultJsonObject.getJsonArray(TERMINAL_ENTRIES));

        if (dischargedForObject.containsKey(VALUE) && dischargedForObject.containsKey(UNIT)) {
            jsonObjectBuilder.add(DISCHARGED_FOR, dischargedForObject);
        } else {
            throw new TransformationException("discharged for is mandatory for conditional discharge");
        }
    }

    public static void handleResultCodeCOLLO(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        resultJsonObject.getJsonArray(TERMINAL_ENTRIES)
                .getValuesAs(JsonObject.class)
                .stream()
                .forEach((e) -> {
                    if (e.containsKey(INDEX) && e.containsKey(VALUE)) {
                        if (e.getInt(INDEX) == 4) {
                            jsonObjectBuilder.add(PAYMENT_TYPE, paymentTypeMap.get(e.getString(VALUE)));
                            jsonObjectBuilder.add(COLLECTION_ORDER_MADE, true);
                        } else if (e.getInt(INDEX) == 9) {
                            jsonObjectBuilder.add(REASON_WHY_NOT_ATTACHED_OR_DEDUCTED, e.getString(VALUE));
                        }
                    }
                });
    }

    public static void handleResultCodeNCOLLO(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(COLLECTION_ORDER_MADE, false);

        resultJsonObject.getJsonArray(TERMINAL_ENTRIES)
                .getValuesAs(JsonObject.class)
                .stream()
                .forEach((e) -> {
                    if (e.containsKey(INDEX)
                            && e.containsKey(VALUE)
                            && e.getInt(INDEX) == 4) {
                            jsonObjectBuilder.add(PAYMENT_TYPE, paymentTypeMap.get(e.getString(VALUE)));
                        }
                    }
                );
    }

    public static void handleResultCodeABDC(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        final Optional<String> totalSum = getValueAsString(resultJsonObject, 1);
        jsonObjectBuilder.add(TOTAL_SUM, new BigDecimal(totalSum.orElseThrow(() -> new TransformationException("VictimSurcharge cannot be blank"))));

        getValueAsString(resultJsonObject, 2).ifPresent(value ->
            jsonObjectBuilder.add(REASON_FOR_DEDUCTING_FROM_BENEFITS, deductFromBenefitsMap.get(value))
        );
    }

    public static void handleResultCodeAEOC(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        final Optional<String> totalSum = getValueAsString(resultJsonObject, 15);
        jsonObjectBuilder.add(TOTAL_SUM, new BigDecimal(totalSum.orElseThrow(() -> new TransformationException("VictimSurcharge cannot be blank"))));

        getValueAsString(resultJsonObject, 10).ifPresent(value ->
            jsonObjectBuilder.add(REASON_FOR_ATTACHING_TO_EARNINGS, deductFromBenefitsMap.get(value))
        );
    }

    public static void handleResultCodeNSP(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        throw new UnsupportedOperationException();
    }

    public static void handleResultCodeNOVS(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        final List<Integer> indexList = new ArrayList<>();
        resultJsonObject
                .getJsonArray(TERMINAL_ENTRIES)
                .getValuesAs(JsonObject.class)
                .forEach((e) -> {
                    if (e.containsKey(INDEX) && e.containsKey(VALUE)) {
                        final int index = e.getInt(INDEX);
                        indexList.add(index);
                        if (index == 5) {
                            jsonObjectBuilder.add(VICTIM_SURCHARGE_REASON_TYPE, e.getString(VALUE));
                        } else if (index == 10) {
                            jsonObjectBuilder.add(VICTIM_SURCHARGE_REASON, e.getString(VALUE));
                        }
                    }
                });

        if (!indexList.contains(5)) {
            throw new TransformationException("Reason for Reduction in victim surcharge or imposed");
        }

        if (!indexList.contains(10)) {
            throw new TransformationException("Reason for no victim surcharge is mandatory");
        }
    }

    public static void handleResultCodeNCR(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(NO_COMPENSATION_REASON, getValueAsString(resultJsonObject, 1)
                .orElseThrow(() -> new TransformationException("No compensation reason is mandatory")));
    }

    public static void handleResultCodeNCOSTS(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        getValueAsString(resultJsonObject, 5)
                .ifPresent(value -> jsonObjectBuilder.add(REASON_FOR_NO_COSTS, value));
    }

    public static void handleResultCodeTFOOUT(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        final List<Integer> indexList = new ArrayList<>();

        resultJsonObject.getJsonArray(TERMINAL_ENTRIES)
                .getValuesAs(JsonObject.class)
                .stream()
                .forEach((e) -> {
                    if (e.containsKey(INDEX) && e.containsKey(VALUE)) {
                        indexList.add(e.getInt(INDEX));
                        if (e.getInt(INDEX) == 1) {
                            jsonObjectBuilder.add(NATIONAL_COURT_NAME, e.getString(VALUE));
                        } else if (e.getInt(INDEX) == -3) {
                            jsonObjectBuilder.add(NATIONAL_COURT_CODE, e.getString(VALUE));
                        }
                    }
                });

        if (!indexList.contains(1)) {
            throw new TransformationException("Transfer to court is mandatory");
        }
    }

    public static void handleResultCodeSUMRCC(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        resultJsonObject.getJsonArray(TERMINAL_ENTRIES)
                .getValuesAs(JsonObject.class)
                .forEach((e) -> {
                    if (e.containsKey(INDEX) && e.containsKey(VALUE)) {
                        if (e.getInt(INDEX) == 5) {
                            jsonObjectBuilder.add(ESTIMATED_HEARING_DURATION, Integer.valueOf(e.getString(VALUE)));
                        } else if (e.getInt(INDEX) == 10) {
                            jsonObjectBuilder.add(LISTING_NOTES, e.getString(VALUE));
                        } else if (e.getInt(INDEX) == -1) {
                            jsonObjectBuilder.add(REFERRAL_REASON_ID, e.getString(VALUE));
                        } else if (e.getInt(INDEX) == 35) {
                            // no need to populate as we don't have a field for the reason text in the new model
                        } else if (e.getInt(INDEX) == -2) {
                            jsonObjectBuilder.add(HEARING_TYPE_ID, e.getString(VALUE));
                        }
                    }
                });
    }

    public static void handleResultCodeRLSUM(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        handleResultCodeLSUM(resultJsonObject, jsonObjectBuilder);
        jsonObjectBuilder.add(RESERVE_TERMS, true);
    }

    public static void handleResultCodeRLSUMI(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        handleResultCodeLSUMI(resultJsonObject, jsonObjectBuilder);
        jsonObjectBuilder.add(RESERVE_TERMS, true);
    }

    public static void handleResultCodeRINSTL(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        handleResultCodeINSTL(resultJsonObject, jsonObjectBuilder);
        jsonObjectBuilder.add(RESERVE_TERMS, true);
    }

    public static void handleResultCodeLSUM(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(RESERVE_TERMS, false);
        final List<Integer> indexList = new ArrayList<>();

        resultJsonObject.getJsonArray(TERMINAL_ENTRIES)
                .getValuesAs(JsonObject.class)
                .forEach((e) -> {
                    if (e.containsKey(INDEX) && e.containsKey(VALUE)) {
                        indexList.add(e.getInt(INDEX));
                        if (e.getInt(INDEX) == 5) {
                            jsonObjectBuilder.add(LUMP_SUM_AMOUNT, new BigDecimal(e.getString(VALUE)));
                        } else if (e.getInt(INDEX) == 6) {
                            jsonObjectBuilder.add(WITHIN_DAYS, lumpSumWithInDaysMap.get(e.getString(VALUE)));
                        }
                    }
                });

        if (!indexList.contains(5)) {
            throw new TransformationException("Total amount  is mandatory");
        }

        if (!indexList.contains(6)) {
            throw new TransformationException("Pay within days is mandatory");
        }
    }

    public static void handleResultCodeLSUMI(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(RESERVE_TERMS, false);
        final List<Integer> indexList = new ArrayList<>();
        resultJsonObject.getJsonArray(TERMINAL_ENTRIES)
                .getValuesAs(JsonObject.class)
                .stream()
                .forEach((e) -> {
                    if (e.containsKey(INDEX) && e.containsKey(VALUE)) {
                        indexList.add(e.getInt(INDEX));
                        if (e.getInt(INDEX) == 96) {
                            jsonObjectBuilder.add(LUMP_SUM_AMOUNT, new BigDecimal(e.getString(VALUE)));
                        } else if (e.getInt(INDEX) == 98) {
                            jsonObjectBuilder.add(INSTALLMENT_AMOUNT, new BigDecimal(e.getString(VALUE)));
                        } else if (e.getInt(INDEX) == 100) {
                            jsonObjectBuilder.add(INSTALLMENT_PERIOD, e.getString(VALUE).toUpperCase());
                        } else if (e.getInt(INDEX) == 99) {
                            String date = e.getString(VALUE);
                            jsonObjectBuilder.add(INSTALLMENT_START_DATE, LocalDate.parse(date, formatter).toString());
                        }
                    }
                });

        if (!indexList.contains(96)) {
            throw new TransformationException("Lumpsumamount  is mandatory");
        }

        if (!indexList.contains(98)) {
            throw new TransformationException("Instalment amount is mandatory");
        }

        if (!indexList.contains(100)) {
            throw new TransformationException("Payment frequency is mandatory");
        }

        if (!indexList.contains(99)) {
            throw new TransformationException("Instalment start date is mandatory");
        }

    }

    public static void handleResultCodeINSTL(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        jsonObjectBuilder.add(RESERVE_TERMS, false);

        final List<Integer> indexList = new ArrayList<>();

        resultJsonObject.getJsonArray(TERMINAL_ENTRIES)
                .getValuesAs(JsonObject.class)
                .forEach((e) -> {
                    if (e.containsKey(INDEX) && e.containsKey(VALUE)) {
                        indexList.add(e.getInt(INDEX));
                        if (e.getInt(INDEX) == 96) {
                            jsonObjectBuilder.add(INSTALLMENT_AMOUNT, new BigDecimal(e.getString(VALUE)));
                        } else if (e.getInt(INDEX) == 97) {
                            jsonObjectBuilder.add(INSTALLMENT_PERIOD, e.getString(VALUE).toUpperCase());
                        } else if (e.getInt(INDEX) == 98) {
                            String date = e.getString(VALUE);
                            jsonObjectBuilder.add(INSTALLMENT_START_DATE, LocalDate.parse(date, formatter).toString());
                        }
                    }
                });

        if (!indexList.contains(96)) {
            throw new TransformationException("Instalment amount is mandatory");
        }

        if (!indexList.contains(97)) {
            throw new TransformationException("Payment frequency is mandatory");
        }

        if (!indexList.contains(98)) {
            throw new TransformationException("Instalment start date is mandatory");
        }
    }

    public static void handleResultCodeSMRTO(final JsonObject resultJsonObject, final JsonObjectBuilder jsonObjectBuilder) {
        resultJsonObject.getJsonArray(TERMINAL_ENTRIES)
                .getValuesAs(JsonObject.class)
                .forEach((e) -> {
                    if (e.containsKey(INDEX) && e.containsKey(VALUE)) {
                        if (e.getInt(INDEX) == 5) {
                            jsonObjectBuilder.add(DATE_OF_HEARING, e.getString(VALUE));
                        } else if (e.getInt(INDEX) == 10) {
                            jsonObjectBuilder.add(TIME_OF_HEARING, e.getString(VALUE));
                        } else if (e.getInt(INDEX) == 15) {
                            jsonObjectBuilder.add(REFERRED_TO_COURT, e.getString(VALUE));
                        } else if (e.getInt(INDEX) == 20) {
                            jsonObjectBuilder.add(MAGISTRATES_COURT, e.getString(VALUE));
                        } else if (e.getInt(INDEX) == 30) {
                            jsonObjectBuilder.add(REFERRED_TO_ROOM, Integer.valueOf(e.getString(VALUE)));
                        } else if (e.getInt(INDEX) == 35) {
                            jsonObjectBuilder.add(REASON, e.getString(VALUE));
                        }
                    }
                });
    }

    private static JsonObject buildDischargedFor(final JsonArray terminalEntries) {
        final JsonObjectBuilder dischargedForJsonObject = createObjectBuilder();
        terminalEntries
                .getValuesAs(JsonObject.class)
                .forEach((e) -> {
                    if (e.containsKey(INDEX) && e.containsKey(VALUE)) {
                        if (e.getInt(INDEX) == 1) {
                            dischargedForJsonObject.add(VALUE, Integer.valueOf(e.getString(VALUE)));
                        } else if (e.getInt(INDEX) == 2) {
                            dischargedForJsonObject.add(UNIT, dischargeForUnitMap.get(e.getString(VALUE)));

                        }
                    }
                });
        return dischargedForJsonObject.build();
    }

    private static Optional<String> getValueAsString(final JsonObject resultJsonObject, final int index) {
        return resultJsonObject
                .getJsonArray(TERMINAL_ENTRIES)
                .getValuesAs(JsonObject.class)
                .stream()
                .filter(e -> e.containsKey(INDEX) && e.containsKey(VALUE) && e.getInt(INDEX) == index)
                .map((e) -> e.getString(VALUE))
                .findFirst();
    }

}
