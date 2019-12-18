package uk.gov.moj.cpp.sjp.query.view.converter.prompts;

import static java.util.Optional.ofNullable;
import static javax.json.Json.createObjectBuilder;
import static jdk.nashorn.internal.runtime.PropertyDescriptor.VALUE;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.PROMPT_DEFINITION_ID;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import java.util.Optional;
import java.util.UUID;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class PromptConverter {

    //The unused property offenceData supplier is used in one of the subclasses
    @SuppressWarnings("squid:S1172")
    public void createPrompt(final JsonArrayBuilder promptsPayloadBuilder, final JsonObject terminalEntry, Prompt prompt, final OffenceDataSupplier offenceDataSupplier){

        final Optional<String> mappedValue = ofNullable(terminalEntry.getString(VALUE, null));
        mappedValue.ifPresent(value -> addPrompt(promptsPayloadBuilder, prompt.getId(), value));
    }

    void addPrompt(final JsonArrayBuilder promptsPayloadBuilder, UUID id, String value) {
        promptsPayloadBuilder.add(createObjectBuilder()
                .add(PROMPT_DEFINITION_ID, id.toString())
                .add(VALUE, value));
    }

}
