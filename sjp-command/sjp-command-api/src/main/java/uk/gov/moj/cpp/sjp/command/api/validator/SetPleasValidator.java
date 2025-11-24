package uk.gov.moj.cpp.sjp.command.api.validator;

import static uk.gov.moj.cpp.sjp.command.api.validator.PleaType.GUILTY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.common.base.Strings;

public class SetPleasValidator {

    public Map<String, List<String>> validate(final SetPleasModel setPleasModel) {

        final Map<String, List<String>> validationErrors = new HashMap<>();

        if (allPleasAreGuilty(setPleasModel)) {
            verifyNoInterpreterOrWelsh(setPleasModel, validationErrors);
        } else {
            // NOT_GUILTY or GUILTY_REQUEST_HEARING or NO PLEA(null)
            verifyInterpreter(setPleasModel, validationErrors);
            verifyWelshHearing(setPleasModel, validationErrors);
        }
        // note that null/other pleas should be taken care of by the JSON schema validation

        return validationErrors;
    }

    private void verifyWelshHearing(final SetPleasModel setPleasModel, final Map<String, List<String>> validationErrors) {
        if (setPleasModel.getSpeakWelsh() == null) {
            addError(validationErrors, UpdatePleaModel.FIELD_SPEAK_WELSH, UpdatePleaValidationErrorMessages.SPEAK_WELSH_NOT_SET);
        }
    }

    private void verifyInterpreter(final SetPleasModel setPleasModel, final Map<String, List<String>> validationErrors) {
        if (setPleasModel.getInterpreterRequired() == null) {
            addError(validationErrors, UpdatePleaModel.FIELD_INTERPRETER_REQUIRED, UpdatePleaValidationErrorMessages.INTERPRETER_REQUIREMENT_NOT_SET);
        } else if (isInterpreterRequired(setPleasModel)) {
            if (Strings.isNullOrEmpty(setPleasModel.getInterpreterLanguage())) {
                addError(validationErrors, UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE, UpdatePleaValidationErrorMessages.INTERPRETER_LANGUAGE_NOT_SET);
            }
        } else if (!isInterpreterRequired(setPleasModel) && !Strings.isNullOrEmpty(setPleasModel.getInterpreterLanguage())) {
            addError(validationErrors, UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE, UpdatePleaValidationErrorMessages.INTERPRETER_LANGUAGE_NOT_ALLOWED);
        }
    }

    private void verifyNoInterpreterOrWelsh(final SetPleasModel setPleasModel, final Map<String, List<String>> validationErrors) {
        if (isInterpreterRequired(setPleasModel)) {
            addError(validationErrors, UpdatePleaModel.FIELD_INTERPRETER_REQUIRED, UpdatePleaValidationErrorMessages.INTERPRETER_NOT_ALLOWED_WHEN_GUILTY);
        }

        if (setPleasModel.getSpeakWelsh()!=null && setPleasModel.getSpeakWelsh()){
            addError(validationErrors, UpdatePleaModel.FIELD_SPEAK_WELSH, UpdatePleaValidationErrorMessages.SPEAK_WELSH_NOT_ALLOWED);
        }

        Optional.ofNullable(setPleasModel.getInterpreterLanguage()).ifPresent(
                language -> addError(validationErrors, UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE, UpdatePleaValidationErrorMessages.INTERPRETER_NOT_ALLOWED_WHEN_GUILTY));
    }

    private boolean allPleasAreGuilty(final SetPleasModel setPleasModel) {
        return setPleasModel.getPleaTypes().stream().
                filter(Objects::nonNull).
                allMatch(pleaType -> pleaType.equals(GUILTY));
    }

    private boolean isInterpreterRequired(final SetPleasModel setPleasModel) {
        return Optional.ofNullable(setPleasModel.getInterpreterRequired()).orElse(false);
    }


    private void addError(Map<String, List<String>> errors, String field, String error) {
        errors.putIfAbsent(field, new ArrayList<>());
        errors.get(field).add(error);
    }
}
