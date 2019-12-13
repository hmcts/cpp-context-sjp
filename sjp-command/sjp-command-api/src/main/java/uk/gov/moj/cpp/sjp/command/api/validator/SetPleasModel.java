package uk.gov.moj.cpp.sjp.command.api.validator;

import static java.util.Collections.EMPTY_LIST;
import static java.util.Optional.ofNullable;

import uk.gov.justice.services.messaging.JsonObjects;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.json.JsonObject;

import org.apache.commons.lang3.EnumUtils;

public class SetPleasModel {

    private static final String FIELD_DEFENDANT_COURT_OPTIONS = "defendantCourtOptions";

    private static final String FIELD_WELSH_HEARING = "welshHearing";

    private static final String FIELD_INTERPRETER = "interpreter";

    private final Boolean interpreterRequired;

    private final String interpreterLanguage;

    private final Boolean speakWelsh;

    private final List<PleaType> pleaTypes;

    public SetPleasModel(final DefendantCourtOptions defendantCourtOptions, final List<PleaType> pleaTypes) {
        Optional<DefendantCourtOptions> courtOptions = Optional.ofNullable(defendantCourtOptions);
        this.speakWelsh = courtOptions.map(DefendantCourtOptions::getWelshHearing).orElse(null);

        Optional<DefendantCourtInterpreter> interpreter = courtOptions.map(DefendantCourtOptions::getInterpreter);
        this.interpreterLanguage = interpreter.map(DefendantCourtInterpreter::getLanguage).orElse(null);
        this.interpreterRequired = interpreter.map(DefendantCourtInterpreter::getNeeded).orElse(null);
        this.pleaTypes = pleaTypes;
    }

    public SetPleasModel(final JsonObject jsonObject) {

        Optional<JsonObject> defendantCourtOptions = getDefendantCourtOptions(jsonObject);
        this.speakWelsh = defendantCourtOptions.map(courtOptions ->
                courtOptions.getBoolean(FIELD_WELSH_HEARING)).orElse(null);

        Optional<JsonObject> interpreter = defendantCourtOptions.map(courtOptions -> courtOptions.getJsonObject(FIELD_INTERPRETER));
        this.interpreterLanguage = interpreter.map(interp -> interp.getString("language", null)).orElse(null);
        this.interpreterRequired = interpreter.map(interp -> interp.getBoolean("needed")).orElse(null);

        this.pleaTypes = ofNullable(jsonObject.getJsonArray("pleas")).
                map(jsonArray -> jsonArray.getValuesAs(JsonObject.class)
                        .stream()
                        .map(e -> EnumUtils.getEnum(PleaType.class, e.getString("pleaType", null)))
                        .collect(Collectors.toList())).
                orElse(EMPTY_LIST);

    }

    private Optional<JsonObject> getDefendantCourtOptions(final JsonObject jsonObject) {
        return JsonObjects.getJsonObject(jsonObject, FIELD_DEFENDANT_COURT_OPTIONS);
    }

    public Boolean getInterpreterRequired() {
        return interpreterRequired;
    }

    public String getInterpreterLanguage() {
        return interpreterLanguage;
    }

    public Boolean getSpeakWelsh() {
        return speakWelsh;
    }

    public List<PleaType> getPleaTypes() {
        return pleaTypes;
    }
}
