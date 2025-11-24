package uk.gov.moj.cpp.sjp.query.view.converter.prompts;


import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class HardcodedValuePromptConverter extends PromptConverter {

    private final String value;

    public HardcodedValuePromptConverter(final String value) {
        this.value = value;
    }

    @Override
    public void createPrompt(final JsonArrayBuilder promptsPayloadBuilder, final JsonObject terminalEntry, final Prompt prompt, final OffenceDataSupplier offenceDataSupplier) {
        addPrompt(promptsPayloadBuilder, prompt.getId(), value);
    }
}
