package uk.gov.moj.cpp.sjp.domain.transformation.converter;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.StringUtils;
import uk.gov.moj.cpp.sjp.domain.transformation.service.SjpViewStoreService;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import static java.util.UUID.randomUUID;
import static javax.json.Json.createArrayBuilder;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.ResultCodeHandler.handleResultCodeSUMRCC;
import static uk.gov.moj.cpp.sjp.domain.transformation.converter.TransformationConstants.*;

public class ReferForCourtHearingConverter implements Converter {

    private static ReferForCourtHearingConverter instance;
    private final SjpViewStoreService sjpViewStoreService;
    public static final String DEFENDANT_COURT_OPTIONS = "defendantCourtOptions";

    private ReferForCourtHearingConverter() {
        this(SjpViewStoreService.getInstance());
    }

    @VisibleForTesting
    ReferForCourtHearingConverter(SjpViewStoreService sjpViewStoreService) {
        this.sjpViewStoreService = sjpViewStoreService;
    }

    public synchronized static final  ReferForCourtHearingConverter getInstance() {
        if (instance == null) {
            instance = new ReferForCourtHearingConverter();
        }
        return instance;
    }

    public JsonObject convert(final JsonObject offenceDecisionJsonObject,
                              final String pleaType,
                              final String verdict) {
        return buildOffenceDecision(offenceDecisionJsonObject, pleaType, verdict);
    }

    private JsonObject buildOffenceDecision(final JsonObject offenceDecisionJsonObject,
                                            final String pleaType,
                                            final String verdict){
        final JsonObject results = convertAllTheResults(offenceDecisionJsonObject);

        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder()
                .add(ID, randomUUID().toString())
                .add(TYPE, DecisionTypeCode.SUMRCC.getResultDecision());

        if (results.containsKey(REFERRAL_REASON_ID)) {
            jsonObjectBuilder.add(REFERRAL_REASON_ID, results.getString(REFERRAL_REASON_ID));
        }

        if (results.containsKey(LISTING_NOTES)) {
            jsonObjectBuilder.add(LISTING_NOTES, results.getString(LISTING_NOTES));
        }

        if (results.containsKey(ESTIMATED_HEARING_DURATION)) {
            jsonObjectBuilder.add(ESTIMATED_HEARING_DURATION, results.getInt(ESTIMATED_HEARING_DURATION));
        }

        final JsonObject defendantCourtOptions = buildDefendantCourtOptions(offenceDecisionJsonObject.getString(ID));

        jsonObjectBuilder.add(DEFENDANT_COURT_OPTIONS, defendantCourtOptions);
        jsonObjectBuilder.add(OFFENCE_DECISION_INFORMATION, createArrayBuilder()
                .add(buildOffenceInformation(offenceDecisionJsonObject.getString(ID), pleaType, verdict)));

        return jsonObjectBuilder.build();
    }

    private JsonObject convertAllTheResults(final JsonObject offenceLevelJsonObject) {
        final JsonObjectBuilder jsonObjectBuilder = createObjectBuilder();

        offenceLevelJsonObject
                .getJsonArray(RESULTS)
                .getValuesAs(JsonObject.class)
                .forEach((eachResultObject) -> {
                    final String resultCode = eachResultObject.getString(CODE);
                    if (resultCode.equalsIgnoreCase(SUMRCC)) {
                        handleResultCodeSUMRCC(eachResultObject, jsonObjectBuilder);
                    }
                });

        return jsonObjectBuilder.build();
    }

    private JsonObject buildOffenceInformation(final String offenceId, final String pleaType, final String verdict) {
        final JsonObjectBuilder offenceInformationObjectBuilder =
                createObjectBuilder().add(OFFENCE_ID, offenceId);

        if(verdict == null) {
            offenceInformationObjectBuilder.add(VERDICT, "GUILTY".equals(pleaType) ? "FOUND_GUILTY" : "PROVED_SJP");
        } else {
            offenceInformationObjectBuilder.add(VERDICT, verdictCodeMap.get(verdict));
        }

        return offenceInformationObjectBuilder.build();
    }

    private JsonObject buildDefendantCourtOptions(String offenceId) {
        final JsonObjectBuilder defendantCourtOptionsObjectBuilder = createObjectBuilder();
        String interpreterLanguage = null;
        Boolean speakWelsh = false;
        if (sjpViewStoreService.getCourtOptions(offenceId) != null) {
            interpreterLanguage = sjpViewStoreService.getCourtOptions(offenceId).getRight();
            speakWelsh = sjpViewStoreService.getCourtOptions(offenceId).getLeft();
        }

        defendantCourtOptionsObjectBuilder.add("welshHearing", Boolean.TRUE.equals(speakWelsh));

        final JsonObjectBuilder interpreterBuilder = createObjectBuilder();
        if (StringUtils.isNotBlank(interpreterLanguage)) {
            interpreterBuilder.add("language", interpreterLanguage);
            interpreterBuilder.add("needed", true);
        } else {
            interpreterBuilder.add("needed", false);
        }
        defendantCourtOptionsObjectBuilder.add("interpreter", interpreterBuilder.build());

        return defendantCourtOptionsObjectBuilder.build();
    }

}
