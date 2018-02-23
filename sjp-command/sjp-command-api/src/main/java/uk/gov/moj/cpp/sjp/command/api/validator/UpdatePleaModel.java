package uk.gov.moj.cpp.sjp.command.api.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;

public class UpdatePleaModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdatePleaModel.class);

    public static final String FIELD_PLEA = "plea";

    public static final String FIELD_INTERPRETER_REQUIRED = "interpreterRequired";

    public static final String FIELD_INTERPRETER_LANGUAGE = "interpreterLanguage";

    private final Boolean interpreterRequired;

    private final String interpreterLanguage;

    private PleaType pleaType;

    public UpdatePleaModel(final JsonObject jsonObject) {
        try {
            this.pleaType = PleaType.valueOf(jsonObject.getString(FIELD_PLEA, ""));
        }
        catch (final IllegalArgumentException e) {
           LOGGER.info("No pleaType provided", e);
           this.pleaType = null;
        }

        this.interpreterLanguage = jsonObject.getString(FIELD_INTERPRETER_LANGUAGE, null);

        this.interpreterRequired = jsonObject.containsKey(FIELD_INTERPRETER_REQUIRED) ?
                jsonObject.getBoolean(FIELD_INTERPRETER_REQUIRED) : null;
    }

    public Boolean getInterpreterRequired() {
        return interpreterRequired;
    }

    public String getInterpreterLanguage() {
        return interpreterLanguage;
    }

    public PleaType getPleaType() {
        return pleaType;
    }
}
