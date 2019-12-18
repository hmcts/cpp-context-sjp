package uk.gov.moj.cpp.sjp.command.api.validator;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.sjp.command.api.validator.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.command.api.validator.PleaType.GUILTY_REQUEST_HEARING;
import static uk.gov.moj.cpp.sjp.command.api.validator.PleaType.NOT_GUILTY;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaModel.FIELD_SPEAK_WELSH;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaValidationErrorMessages.INTERPRETER_LANGUAGE_NOT_ALLOWED;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaValidationErrorMessages.INTERPRETER_LANGUAGE_NOT_SET;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaValidationErrorMessages.INTERPRETER_NOT_ALLOWED_WHEN_GUILTY;
import static uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaValidationErrorMessages.SPEAK_WELSH_NOT_SET;

import uk.gov.moj.cpp.sjp.domain.DefendantCourtInterpreter;
import uk.gov.moj.cpp.sjp.domain.DefendantCourtOptions;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SetPleasValidatorTest {

    private final SetPleasValidator service = new SetPleasValidator();

    @Test
    public void shouldAllowValidGuiltyPleaWithoutCourtOptions() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(null, false),
                newArrayList(GUILTY));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors, equalTo(emptyMap()));
    }

    @Test
    public void shouldAllowValidNotGuiltyPlea() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(
                    new DefendantCourtInterpreter(null, false), false),
                newArrayList(NOT_GUILTY));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors, equalTo(emptyMap()));
    }

    @Test
    public void shouldAllowValidGuiltyRequestHearingPlea() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("Greek", true),false
                ), newArrayList(GUILTY_REQUEST_HEARING));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors, equalTo(emptyMap()));
    }

    @Test
    public void shouldReturnGuiltyPleaErrorMessages() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("Basque", false),false
                ), newArrayList(GUILTY));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_INTERPRETER_LANGUAGE), contains(INTERPRETER_NOT_ALLOWED_WHEN_GUILTY));
    }


    @Test
    public void shouldReturnErrorMessage_INTERPRETER_LANGUAGE_NOT_SET_nullValue() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter(null, true), false
                ), newArrayList(GUILTY_REQUEST_HEARING));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_INTERPRETER_LANGUAGE), contains(INTERPRETER_LANGUAGE_NOT_SET));
    }

    @Test
    public void shouldReturnErrorMessage_INTERPRETER_LANGUAGE_NOT_SET_emptyString() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(
                    new DefendantCourtInterpreter("", true), false
                ), newArrayList(GUILTY_REQUEST_HEARING));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_INTERPRETER_LANGUAGE), contains(INTERPRETER_LANGUAGE_NOT_SET));
    }

    @Test
    public void shouldReturnErrorMessage_INTERPRETER_LANGUAGE_NOT_ALLOWED() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("French", false), false
                ), newArrayList(GUILTY_REQUEST_HEARING));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_INTERPRETER_LANGUAGE), contains(INTERPRETER_LANGUAGE_NOT_ALLOWED));
    }

    @Test
    public void shouldReturnErrorMessage_SPEAK_WELSH_NOT_SET() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(
                        new DefendantCourtInterpreter("", false), null
                ), newArrayList(GUILTY_REQUEST_HEARING));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors.get(FIELD_SPEAK_WELSH), contains(SPEAK_WELSH_NOT_SET));
    }

    @Test
    public void shouldNotRequireInterpreter() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(null, false),
                newArrayList(GUILTY, GUILTY));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors, equalTo(emptyMap()));
    }

    @Test
    public void shouldAllowValidMixedPleas() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(new DefendantCourtInterpreter(null, false), false),
                newArrayList(GUILTY, NOT_GUILTY));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors, equalTo(emptyMap()));
    }

    @Test
    public void shouldAcceptNoPleas() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(null, false),
                newArrayList(GUILTY, null));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors, equalTo(emptyMap()));
    }

    @Test
    public void shouldAcceptGuiltyAndNullPleas() {
        final SetPleasModel data = new SetPleasModel(
                new DefendantCourtOptions(null, false),
                newArrayList(GUILTY, GUILTY, null));
        final Map<String, List<String>> errors = whenUpdatePleaValidatorIsCalled(data);

        assertThat(errors, equalTo(emptyMap()));
    }


    private Map<String, List<String>> whenUpdatePleaValidatorIsCalled(final SetPleasModel data) {
        return service.validate(data);
    }
}
