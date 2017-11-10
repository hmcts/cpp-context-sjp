package uk.gov.moj.cpp.sjp.command.api.validator;

public abstract class UpdatePleaValidationErrorMessages {

    public static final String INTERPRETER_NOT_ALLOWED_WHEN_GUILTY = "Cannot require an interpreter when pleading guilty";

    public static final String INTERPRETER_NOT_ALLOWED_WHEN_NO_PLEA = "Cannot require an interpreter when there is no plea";

    public static final String INTERPRETER_REQUIREMENT_NOT_SET = "Please choose whether an interpreter is needed";

    public static final String INTERPRETER_LANGUAGE_NOT_SET = "Please choose an interpreter language";

    public static final String INTERPRETER_LANGUAGE_NOT_ALLOWED = "Cannot choose an interpreter language if an interpreter is not needed";

    private UpdatePleaValidationErrorMessages() {
        // prevent initialization
    }

}
