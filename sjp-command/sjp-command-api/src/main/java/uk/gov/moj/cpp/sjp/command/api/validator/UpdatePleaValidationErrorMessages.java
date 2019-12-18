package uk.gov.moj.cpp.sjp.command.api.validator;

public abstract class UpdatePleaValidationErrorMessages {

    public static final String INTERPRETER_NOT_ALLOWED_WHEN_GUILTY = "Cannot require an interpreter when pleading guilty";

    public static final String INTERPRETER_REQUIREMENT_NOT_SET = "Please choose whether an interpreter is needed";

    public static final String INTERPRETER_LANGUAGE_NOT_SET = "Please choose an interpreter language";

    public static final String INTERPRETER_LANGUAGE_NOT_ALLOWED = "Cannot choose an interpreter language if an interpreter is not needed";

    public static final String SPEAK_WELSH_NOT_SET = "Please choose whether the defendant wishes to speak Welsh";

    public static final String SPEAK_WELSH_NOT_ALLOWED = "Cannot require a welsh hearing when pleading guilty";

    private UpdatePleaValidationErrorMessages() {
        // prevent initialization
    }

}
