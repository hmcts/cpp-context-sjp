package uk.gov.moj.cpp.sjp.command.api.validator;

import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.cpp.sjp.command.api.validator.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.command.api.validator.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.command.api.validator.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaModel.FIELD_INTERPRETER_REQUIRED;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaModel.FIELD_SPEAK_WELSH;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaValidationErrorMessages.INTERPRETER_LANGUAGE_NOT_ALLOWED;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaValidationErrorMessages.INTERPRETER_LANGUAGE_NOT_SET;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaValidationErrorMessages.INTERPRETER_NOT_ALLOWED_WHEN_GUILTY;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaValidationErrorMessages.INTERPRETER_REQUIREMENT_NOT_SET;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaValidationErrorMessages.SPEAK_WELSH_NOT_SET;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test for UpdatePleaValidator.
 */
@ExtendWith(MockitoExtension.class)
public class UpdatePleaValidatorTest {

    private final UpdatePleaValidator service = new UpdatePleaValidator();

    @Test
    public void shouldAllowValidGuiltyPlea() {
        final UpdatePleaModel data = new UpdatePleaModel(GUILTY, null, null, null);
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors, equalTo(emptyMap()));
    }

    @Test
    public void shouldAllowValidNotGuiltyPlea() {
        final UpdatePleaModel data = new UpdatePleaModel(NOT_GUILTY, false, null, false);
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors, equalTo(emptyMap()));
    }

    @Test
    public void shouldAllowValidGuiltyRequestHearingPlea() {
        final UpdatePleaModel data = new UpdatePleaModel(GUILTY_REQUEST_HEARING, true, "Greek", false);
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors, equalTo(emptyMap()));
    }

    @Test
    public void shouldReturnGuiltyPleaErrorMessages() {
        final UpdatePleaModel data = new UpdatePleaModel(GUILTY, false, "Basque", false);
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_INTERPRETER_REQUIRED), contains(INTERPRETER_NOT_ALLOWED_WHEN_GUILTY));
        assertThat(errors.get(FIELD_INTERPRETER_LANGUAGE), contains(INTERPRETER_NOT_ALLOWED_WHEN_GUILTY));
    }

    @Test
    public void shouldReturnErrorMessage_INTERPRETER_REQUIREMENT_NOT_SET() {
        final UpdatePleaModel data = new UpdatePleaModel(NOT_GUILTY, null, null, false);
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_INTERPRETER_REQUIRED), contains(INTERPRETER_REQUIREMENT_NOT_SET));
    }

    @Test
    public void shouldReturnErrorMessage_INTERPRETER_LANGUAGE_NOT_SET_nullValue() {
        final UpdatePleaModel data = new UpdatePleaModel(GUILTY_REQUEST_HEARING, true, null, false);
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_INTERPRETER_LANGUAGE), contains(INTERPRETER_LANGUAGE_NOT_SET));
    }

    @Test
    public void shouldReturnErrorMessage_INTERPRETER_LANGUAGE_NOT_SET_emptyString() {
        final UpdatePleaModel data = new UpdatePleaModel(GUILTY_REQUEST_HEARING, true, "", false);
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_INTERPRETER_LANGUAGE), contains(INTERPRETER_LANGUAGE_NOT_SET));
    }

    @Test
    public void shouldReturnErrorMessage_INTERPRETER_LANGUAGE_NOT_ALLOWED() {
        final UpdatePleaModel data = new UpdatePleaModel(GUILTY_REQUEST_HEARING, false, "", false);
        final  Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_INTERPRETER_LANGUAGE), contains(INTERPRETER_LANGUAGE_NOT_ALLOWED));
    }

    @Test
    public void shouldReturnErrorMessage_SPEAK_WELSH_NOT_SET() {
        final UpdatePleaModel data = new UpdatePleaModel(GUILTY_REQUEST_HEARING, false, "", null);
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_SPEAK_WELSH), contains(SPEAK_WELSH_NOT_SET));
    }

    @Test
    public void shouldReturnMultipleErrorMessages() {
        final UpdatePleaModel data = new UpdatePleaModel(GUILTY_REQUEST_HEARING, true, null, null);
        final  Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_INTERPRETER_LANGUAGE), contains(INTERPRETER_LANGUAGE_NOT_SET));
        assertThat(errors.get(FIELD_SPEAK_WELSH), contains(SPEAK_WELSH_NOT_SET));
    }

    private Map<String, List<String>> whenUpdatePleaValidatorIsCalled(final UpdatePleaModel data) {
        return service.validate(data);
    }

}
