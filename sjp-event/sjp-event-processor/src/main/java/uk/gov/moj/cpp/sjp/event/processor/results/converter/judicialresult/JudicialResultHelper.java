package uk.gov.moj.cpp.sjp.event.processor.results.converter.judicialresult;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.UUID.fromString;
import static java.util.stream.Collectors.joining;
import static uk.gov.justice.core.courts.JudicialResult.judicialResult;

import uk.gov.justice.core.courts.JudicialResult;
import uk.gov.justice.core.courts.JudicialResultCategory;
import uk.gov.justice.core.courts.JudicialResultPrompt;
import uk.gov.justice.core.courts.SecondaryCJSCode;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.json.JsonObject;
import javax.json.JsonString;

import com.google.common.collect.ImmutableMap;

public class JudicialResultHelper {

    public static final String STARTED_AT = "startedAt";
    public static final String LABEL = "label";
    public static final String ADJOURNMENT = "adjournment";
    public static final String FINANCIAL = "financial";
    public static final String CONVICTED = "convicted";
    public static final String IS_AVAILABLE_FOR_COURT_EXTRACT = "isAvailableForCourtExtract";
    public static final String ORDERED_DATE = "orderedDate";
    public static final String CATEGORY = "category";
    public static final String TERMINATES_OFFENCE_PROCEEDINGS = "terminatesOffenceProceedings";
    public static final String LIFE_DURATION = "lifeDuration";
    public static final String PUBLISHED_FOR_NOWS = "publishedForNows";
    public static final String ROLL_UP_PROMPTS = "rollUpPrompts";
    public static final String PUBLISHED_AS_A_PROMPT = "publishedAsAPrompt";
    public static final String EXCLUDED_FROM_RESULTS = "excludedFromResults";
    public static final String ALWAYS_PUBLISHED = "alwaysPublished";
    public static final String POINTS_DISQUALIFICATION_CODE = "pointsDisqualificationCode";
    public static final String URGENT = "urgent";
    public static final String D_20 = "d20";
    public static final String SESSION_ID = "sessionId";
    private static final String WELSH_LABEL = "welshLabel";
    private static final String UNSCHEDULED = "unscheduled";
    private static final String RESULT_WORDING = "resultWording";
    private static final String WELSH_RESULT_WORDING = "welshResultWording";
    private static final String CJS_CODE = "cjsCode";
    private static final String CAN_BE_SUBJECT_OF_BREACH = "canBeSubjectOfBreach";
    private static final String CAN_BE_SUBJECT_OF_VARIATION = "canBeSubjectOfVariation";
    private static final String SECONDARY_CJS_CODES = "secondaryCJSCodes";
    private static final String TEXT = "text";
    private static final String LEVEL = "level";
    private static final String POST_HEARING_CUSTODY_STATUS = "postHearingCustodyStatus";
    private static final String RESULT_DEFINITION_GROUP = "resultDefinitionGroup";
    private static final String RANK = "rank";

    public static final Map<String, String> categoryMap = ImmutableMap.of(
            "A", "ANCILLARY",
            "F", "FINAL",
            "I", "INTERMEDIARY");

    public static final String Y = "Y";


    private JudicialResultHelper() {
    }

    public static JudicialResultPrompt.Builder populatePromptDefinitionAttributes(final JPrompt jsPrompt,
                                                                                  final JsonObject resultDefinition) {
        final Optional<JsonObject> promptJson =
                resultDefinition.getJsonArray("prompts")
                .getValuesAs(JsonObject.class)
                .stream()
                        .filter(prompt -> (jsPrompt.getPromptReference() == null
                                && prompt.getString("id").equals(jsPrompt.getId().toString()))
                                || (prompt.getString("id").equals(jsPrompt.getId().toString())
                                && jsPrompt.getPromptReference().equals(prompt.getString("reference"))))
                .findFirst();

        return populatePrompt(jsPrompt, promptJson);

    }

    public static JudicialResultPrompt.Builder populatePromptDefinitionAttributesBasedOnDuration(final JPrompt jsPrompt,
                                                                                                 final JsonObject resultDefinition,
                                                                                                 final String duration) {
        final Optional<JsonObject> promptJson =
                resultDefinition.getJsonArray("prompts")
                        .getValuesAs(JsonObject.class)
                        .stream()
                        .filter(prompt -> prompt.getString("id").equals(jsPrompt.getId().toString())
                                && duration.equals(prompt.getString("duration")))
                        .findFirst();

        return populatePrompt(jsPrompt, promptJson);
    }

    @SuppressWarnings("squid:S00112")
    private static JudicialResultPrompt.Builder populatePrompt(final JPrompt jsPrompt,
                                                               final Optional<JsonObject> promptJson) {
        if (promptJson.isPresent()) {
            final JsonObject promptJsonObject = promptJson.get();
            return JudicialResultPrompt.judicialResultPrompt()
                    .withJudicialResultPromptTypeId(fromString(promptJsonObject.getString("id")))
                    .withDurationSequence(promptJsonObject.getInt("durationSequence", 0))
                    .withCourtExtract(promptJsonObject.getString("courtExtract", null))
                    .withLabel(promptJsonObject.getString(LABEL, null))
                    .withWelshLabel(promptJsonObject.getString(WELSH_LABEL, null))
                    .withIsFinancialImposition(!"N".equalsIgnoreCase(promptJsonObject.getString(FINANCIAL, null)))
                    .withPromptSequence(new BigDecimal(promptJsonObject.getInt("sequence")))
                    .withPromptReference(promptJsonObject.getString("reference", null))
                    .withType(promptJsonObject.getString("type", null))
                    .withUsergroups(
                            promptJsonObject.
                                    getJsonArray("userGroups")
                                    .getValuesAs(JsonString.class)
                                    .stream()
                                    .map(JsonString::getString)
                                    .collect(Collectors.toList()));

        } else {
            throw new RuntimeException(format("Unable to find Prompt for prompt Id %s", jsPrompt.getId()));
        }
    }

    @SuppressWarnings({"squid:S3776", "MethodCyclomaticComplexity"})
    public static JudicialResult.Builder populateResultDefinitionAttributes(final UUID resultId,
                                                                            final UUID sessionId,
                                                                            final JsonObject resultDefinition) {

        return judicialResult()
                .withJudicialResultId(resultId)
                .withJudicialResultTypeId(resultId)
                .withOrderedHearingId(sessionId)
                .withCjsCode(resultDefinition.getString(CJS_CODE, null))
                .withSecondaryCJSCodes(resultDefinition.containsKey(SECONDARY_CJS_CODES) ? resultDefinition.getJsonArray(SECONDARY_CJS_CODES).getValuesAs(JsonObject.class)
                        .stream()
                        .map(secondaryCJSCode -> SecondaryCJSCode.secondaryCJSCode()
                                .withCjsCode(secondaryCJSCode.getString(CJS_CODE, null))
                                .withText(secondaryCJSCode.getString(TEXT, null))
                                .build())
                        .collect(Collectors.toList()) : null)
                .withLevel(resultDefinition.getString(LEVEL, null))
                .withPostHearingCustodyStatus(resultDefinition.getString(POST_HEARING_CUSTODY_STATUS, null))
                .withResultDefinitionGroup(resultDefinition.getString(RESULT_DEFINITION_GROUP, null))
                .withRank(resultDefinition.containsKey(RANK) ? new BigDecimal(resultDefinition.getInt(RANK)) : null)
                .withLifeDuration(resultDefinition.getBoolean(LIFE_DURATION, false))
                .withCanBeSubjectOfBreach(resultDefinition.getBoolean(CAN_BE_SUBJECT_OF_BREACH, false))
                .withCanBeSubjectOfVariation(resultDefinition.getBoolean(CAN_BE_SUBJECT_OF_VARIATION, false))
                .withResultWording(resultDefinition.getString(RESULT_WORDING, null))
                .withWelshResultWording(resultDefinition.getString(WELSH_RESULT_WORDING, null))
                .withLabel(resultDefinition.getString(LABEL, null))
                .withWelshLabel(resultDefinition.getString(WELSH_LABEL, null))
                .withIsUnscheduled(resultDefinition.getBoolean(UNSCHEDULED, false))
                .withIsAdjournmentResult(Y.equalsIgnoreCase(resultDefinition.getString(ADJOURNMENT, null)))
                .withIsFinancialResult(Y.equalsIgnoreCase(resultDefinition.getString(FINANCIAL, null)))
                .withIsConvictedResult(Y.equalsIgnoreCase(resultDefinition.getString(CONVICTED, null)))
                .withIsAvailableForCourtExtract(resultDefinition.getBoolean(IS_AVAILABLE_FOR_COURT_EXTRACT, false))
                .withOrderedDate(resultDefinition.getString(ORDERED_DATE, null))
                .withCategory(resultDefinition.containsKey(CATEGORY) ? JudicialResultCategory.valueFor(categoryMap.get(resultDefinition.getString(CATEGORY))).orElse(null) : null)
                .withTerminatesOffenceProceedings(resultDefinition.getBoolean(TERMINATES_OFFENCE_PROCEEDINGS, false))
                .withLifeDuration(resultDefinition.getBoolean(LIFE_DURATION, false))
                .withPublishedForNows(resultDefinition.getBoolean(PUBLISHED_FOR_NOWS, false))
                .withRollUpPrompts(resultDefinition.getBoolean(ROLL_UP_PROMPTS, false))
                .withPublishedAsAPrompt(resultDefinition.getBoolean(PUBLISHED_AS_A_PROMPT, false))
                .withExcludedFromResults(resultDefinition.getBoolean(EXCLUDED_FROM_RESULTS, false))
                .withAlwaysPublished(resultDefinition.getBoolean(ALWAYS_PUBLISHED, false))
                .withUrgent(resultDefinition.getBoolean(URGENT, false))
                .withPointsDisqualificationCode(resultDefinition.getString(POINTS_DISQUALIFICATION_CODE, null))
                .withD20(resultDefinition.getBoolean(D_20, false));
    }


    public static String getResultText(final List<JudicialResultPrompt> judicialResultPrompts,
                                       final String resultLabel) {
        if (!judicialResultPrompts.isEmpty()) {
            final String sortedPrompts = judicialResultPrompts
                    .stream()
                    .map(p -> format("%s %s", p.getLabel(), p.getValue()))
                    .collect(joining(lineSeparator()));
            return format("%s%s%s", resultLabel, lineSeparator(), sortedPrompts);
        }
        return resultLabel;
    }



}
