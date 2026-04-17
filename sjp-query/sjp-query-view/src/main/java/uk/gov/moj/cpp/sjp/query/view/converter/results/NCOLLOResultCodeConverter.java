package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static com.google.common.collect.ImmutableList.copyOf;
import static jakarta.json.Json.createArrayBuilder;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import java.util.Arrays;
import java.util.List;

import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

public class NCOLLOResultCodeConverter extends ResultCodeConverter {

    private static final List<Prompt> promptList = Arrays.asList(Prompt.NCOLLO_REASON);

    @Override
    public List<Prompt> getPromptList() {
        return copyOf(promptList);
    }

    @Override
    public JsonArrayBuilder createPrompts(final JsonObject result, final OffenceDataSupplier offenceDataSupplier) {
        final JsonArrayBuilder promptsPayloadBuilder = createArrayBuilder();

        Prompt.NCOLLO_REASON.createPrompt(promptsPayloadBuilder, null, offenceDataSupplier);

        return promptsPayloadBuilder;
    }
}
