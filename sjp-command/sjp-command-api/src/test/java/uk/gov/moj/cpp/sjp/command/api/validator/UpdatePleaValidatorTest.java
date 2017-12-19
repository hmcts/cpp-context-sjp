package uk.gov.moj.cpp.sjp.command.api.validator;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObjectBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Unit test for UpdatePleaValidator.
 */
@RunWith(MockitoJUnitRunner.class)
public class UpdatePleaValidatorTest {

    private final UpdatePleaValidator service = new UpdatePleaValidator();

    @Test
    public void shouldAllowValidGuiltyPlea() {
        // given
        final UpdatePleaModel data = prepareData(PleaType.GUILTY, null, null);

        // when
        Map<String, List<String>> errors = service.validate(data);

        // then
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void shouldAllowValidNotGuiltyPlea() {
        // given
        final UpdatePleaModel data = prepareData(PleaType.NOT_GUILTY, false, null);

        // when
        Map<String, List<String>> errors = service.validate(data);

        // then
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void shouldAllowValidGuiltyRequestHearingPlea() {
        // given
        final UpdatePleaModel data = prepareData(PleaType.GUILTY_REQUEST_HEARING, true, "Greek");

        // when
        Map<String, List<String>> errors = service.validate(data);

        // then
        assertThat(errors.isEmpty(), is(true));
    }

    @Test
    public void shouldReturnGuiltyPleaErrorMessages() {
        // given
        final UpdatePleaModel data = prepareData(PleaType.GUILTY, false, "Basque");

        // when
        Map<String, List<String>> errors = service.validate(data);

        // then
        assertThat(errors.get(UpdatePleaModel.FIELD_INTERPRETER_REQUIRED),
                is(Collections.singletonList(UpdatePleaValidationErrorMessages.INTERPRETER_NOT_ALLOWED_WHEN_GUILTY)));
        assertThat(errors.get(UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE),
                is(Collections.singletonList(UpdatePleaValidationErrorMessages.INTERPRETER_NOT_ALLOWED_WHEN_GUILTY)));
    }

    @Test
    public void shouldReturnErrorMessage_INTERPRETER_REQUIREMENT_NOT_SET() {
        // given
        final UpdatePleaModel data = prepareData(PleaType.NOT_GUILTY, null, null);

        // when
        Map<String, List<String>> errors = service.validate(data);

        // then
        assertThat(errors.get(UpdatePleaModel.FIELD_INTERPRETER_REQUIRED),
                is(Collections.singletonList(UpdatePleaValidationErrorMessages.INTERPRETER_REQUIREMENT_NOT_SET)));
    }

    @Test
    public void shouldReturnErrorMessage_INTERPRETER_LANGUAGE_NOT_SET_nullValue() {
        // given
        final UpdatePleaModel data = prepareData(PleaType.GUILTY_REQUEST_HEARING, true, null);

        // when
        Map<String, List<String>> errors = service.validate(data);

        // then
        assertThat(errors.get(UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE),
                is(Collections.singletonList(UpdatePleaValidationErrorMessages.INTERPRETER_LANGUAGE_NOT_SET)));
    }

    @Test
    public void shouldReturnErrorMessage_INTERPRETER_LANGUAGE_NOT_SET_emptyString() {
        // given
        final UpdatePleaModel data = prepareData(PleaType.GUILTY_REQUEST_HEARING, true, "");

        // when
        Map<String, List<String>> errors = service.validate(data);

        // then
        assertThat(errors.get(UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE),
                is(Collections.singletonList(UpdatePleaValidationErrorMessages.INTERPRETER_LANGUAGE_NOT_SET)));
    }

    @Test
    public void shouldReturnErrorMessage_INTERPRETER_LANGUAGE_NOT_ALLOWED() {
        // given
        final UpdatePleaModel data = prepareData(PleaType.GUILTY_REQUEST_HEARING, false, "");

        // when
        Map<String, List<String>> errors = service.validate(data);

        // then
        assertThat(errors.get(UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE),
                is(Collections.singletonList(UpdatePleaValidationErrorMessages.INTERPRETER_LANGUAGE_NOT_ALLOWED)));
    }


    private UpdatePleaModel prepareData(PleaType plea, Boolean required, String language) {
        JsonObjectBuilder builder = Json.createObjectBuilder();

        if (plea != null) {
            builder.add(UpdatePleaModel.FIELD_PLEA, plea.name());
        }

        if (required != null) {
            builder.add(UpdatePleaModel.FIELD_INTERPRETER_REQUIRED, required);
        }

        if (language != null) {
            builder.add(UpdatePleaModel.FIELD_INTERPRETER_LANGUAGE, language);
        }

        return new UpdatePleaModel(builder.build());
    }
}
