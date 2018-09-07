package uk.gov.moj.cpp.sjp.command.api.validator;

import java.util.Optional;

import javax.json.JsonObject;

import org.apache.commons.lang3.EnumUtils;

public class UpdatePleaModel {

    private static final String FIELD_PLEA = "plea";

    static final String FIELD_INTERPRETER_REQUIRED = "interpreterRequired";

    static final String FIELD_INTERPRETER_LANGUAGE = "interpreterLanguage";

    private PleaType pleaType;

    private final Boolean interpreterRequired;

    private final String interpreterLanguage;

    UpdatePleaModel(final PleaType pleaType, final Boolean interpreterRequired, final String interpreterLanguage) {
        this.pleaType = pleaType;
        this.interpreterRequired = interpreterRequired;
        this.interpreterLanguage = interpreterLanguage;
    }

    public UpdatePleaModel(final JsonObject jsonObject) {
        this(
                EnumUtils.getEnum(PleaType.class, jsonObject.getString(FIELD_PLEA, null)),
                extractBooleanOrNull(jsonObject, FIELD_INTERPRETER_REQUIRED),
                jsonObject.getString(FIELD_INTERPRETER_LANGUAGE, null));
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

    private static Boolean extractBooleanOrNull(final JsonObject jsonObject, final String key) {
        return Optional.of(key)
                .filter(jsonObject::containsKey)
                .map(jsonObject::getBoolean)
                .orElse(null);
    }

}
