package uk.gov.moj.cpp.sjp.command.api.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;

public class UpdatePleaValidator {

    public Map<String, List<String>> validate(final UpdatePleaModel updatePleaModel) {

        final Map<String, List<String>> validationErrors = new HashMap<>();

        if (UpdatePleaModel.PleaType.GUILTY.equals(updatePleaModel.getPleaType())) {
            if (updatePleaModel.getInterpreterRequired() != null) {
                addError(validationErrors, UpdatePleaModel.FIELD_INTERPRETER_REQUIRED, UpdatePleaValidationErrorMessages.INTERPRETER_NOT_ALLOWED_WHEN_GUILTY);
            }
            if (updatePleaModel.getInterpreterLanguage() != null) {
                addError(validationErrors, UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE, UpdatePleaValidationErrorMessages.INTERPRETER_NOT_ALLOWED_WHEN_GUILTY);
            }
        }
        else {
            // NOT_GUILTY or GUILTY_REQUEST_HEARING
            if (updatePleaModel.getInterpreterRequired() == null) {
                addError(validationErrors, UpdatePleaModel.FIELD_INTERPRETER_REQUIRED, UpdatePleaValidationErrorMessages.INTERPRETER_REQUIREMENT_NOT_SET);
            }
            else if (updatePleaModel.getInterpreterRequired()) {
                if (Strings.isNullOrEmpty(updatePleaModel.getInterpreterLanguage())) {
                    addError(validationErrors, UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE, UpdatePleaValidationErrorMessages.INTERPRETER_LANGUAGE_NOT_SET);
                }
            } else {
                if (updatePleaModel.getInterpreterLanguage() != null) {
                    addError(validationErrors, UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE, UpdatePleaValidationErrorMessages.INTERPRETER_LANGUAGE_NOT_ALLOWED);
                }
            }
        }
        // note that null/other pleas should be taken care of by the JSON schema validation

        return validationErrors;
    }


    private void addError(Map<String, List<String>> errors, String field, String error) {
        errors.putIfAbsent(field, new ArrayList<>());
        errors.get(field).add(error);
    }
}
