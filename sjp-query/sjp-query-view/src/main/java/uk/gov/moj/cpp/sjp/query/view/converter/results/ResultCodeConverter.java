package uk.gov.moj.cpp.sjp.query.view.converter.results;

import static java.util.stream.Collectors.toList;
import static javax.json.Json.createArrayBuilder;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.INDEX;
import static uk.gov.moj.cpp.sjp.query.view.util.CaseResultsConstants.TERMINAL_ENTRIES;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import java.util.List;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public abstract class ResultCodeConverter {

    public JsonArrayBuilder createPrompts(final JsonObject result, final OffenceDataSupplier offenceDataSupplier) {

        final JsonArrayBuilder promptsPayloadBuilder = createArrayBuilder();

        if (result.containsKey(TERMINAL_ENTRIES)) {
            result.getJsonArray(TERMINAL_ENTRIES).getValuesAs(JsonObject.class)
                    .forEach(
                            terminalEntry -> getMatchingPromptList(terminalEntry)
                                    .forEach(
                                            prompt -> prompt.createPrompt(promptsPayloadBuilder, terminalEntry, offenceDataSupplier)
                                    )
                    );
        }

        return promptsPayloadBuilder;
    }

    private List<Prompt> getMatchingPromptList(final JsonObject terminalEntry) {
        final Integer index = terminalEntry.getInt(INDEX, 99999);

        return getPromptList().stream().filter(p -> index.equals(p.getIndex())).collect(toList());
    }

    protected abstract List<Prompt> getPromptList();

}
