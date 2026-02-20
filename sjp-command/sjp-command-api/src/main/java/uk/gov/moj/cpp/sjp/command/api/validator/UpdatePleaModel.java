package uk.gov.moj.cpp.sjp.command.api.validator;


import javax.json.JsonObject;

import org.apache.commons.lang3.EnumUtils;

public class UpdatePleaModel {

    private static final String FIELD_PLEA = "plea";

    static final String FIELD_INTERPRETER_REQUIRED = "interpreterRequired";

    static final String FIELD_INTERPRETER_LANGUAGE = "interpreterLanguage";

    static final String FIELD_SPEAK_WELSH = "speakWelsh";

    private final PleaType pleaType;

    private final Boolean interpreterRequired;

    private final String interpreterLanguage;

    private final Boolean speakWelsh;

    UpdatePleaModel(final PleaType pleaType, final Boolean interpreterRequired,
                    final String interpreterLanguage, final Boolean speakWelsh) {
        this.pleaType = pleaType;
        this.interpreterRequired = interpreterRequired;
        this.interpreterLanguage = interpreterLanguage;
        this.speakWelsh = speakWelsh;
    }

    public UpdatePleaModel(final JsonObject jsonObject) {
        this(
                EnumUtils.getEnum(PleaType.class, jsonObject.getString(FIELD_PLEA, null)),
                JsonObjects.getBoolean(jsonObject, FIELD_INTERPRETER_REQUIRED).orElse(null),
                jsonObject.getString(FIELD_INTERPRETER_LANGUAGE, null),
                JsonObjects.getBoolean(jsonObject, FIELD_SPEAK_WELSH).orElse(null));
    }

    PleaType getPleaType() {
        return pleaType;
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

}
