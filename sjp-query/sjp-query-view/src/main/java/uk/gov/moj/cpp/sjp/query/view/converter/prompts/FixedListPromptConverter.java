package uk.gov.moj.cpp.sjp.query.view.converter.prompts;

import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.VALUE;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;
import uk.gov.moj.cpp.sjp.query.view.converter.results.FixedList;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import java.util.Optional;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class FixedListPromptConverter extends PromptConverter {

    private final FixedList fixedList;

    public FixedListPromptConverter(final FixedList fixedList) {
        this.fixedList = fixedList;
    }

    @Override
    public void createPrompt(final JsonArrayBuilder promptsPayloadBuilder, final JsonObject terminalEntry, final Prompt prompt, final OffenceDataSupplier offenceDataSupplier) {
        final Optional<String> fixedListValue = ofNullable(terminalEntry.getString(VALUE, null)).flatMap(fixedList::mapValue);

        fixedListValue.ifPresent(value -> addPrompt(promptsPayloadBuilder, prompt.getId(), value));
    }
}
