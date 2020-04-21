package uk.gov.moj.cpp.sjp.query.view.converter.prompts;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static jdk.nashorn.internal.runtime.PropertyDescriptor.VALUE;

import uk.gov.moj.cpp.sjp.query.view.converter.FixedListConverterUtil;
import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;
import uk.gov.moj.cpp.sjp.query.view.converter.results.FixedList;
import uk.gov.moj.cpp.sjp.query.view.response.CaseView;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import java.util.Optional;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class MajorCreditorPromptConverter extends PromptConverter{


    private Optional<String> mapProsecutionAuthority(final OffenceDataSupplier offenceDataSupplier){

        final CaseView caseView = offenceDataSupplier.getCaseView();
        final Optional<JsonObject> allFixedListResult = offenceDataSupplier.getAllFixedList();

        final Optional<JsonObject> referenceDataFixedList = mapToReferenceDataFixedList(allFixedListResult);
        if (referenceDataFixedList.isPresent()) {
            final Optional<JsonObject> matchedList = referenceDataFixedList.get().getJsonArray("elements").getValuesAs(JsonObject.class).stream()
                    .filter(fixedList -> fixedList.getString("code").startsWith(caseView.getProsecutingAuthority())).findFirst();
            if (matchedList.isPresent()) {
                return ofNullable(matchedList.get().getString(VALUE, null));
            }
        }

        return empty();
    }

    private Optional<JsonObject> mapToReferenceDataFixedList(final Optional<JsonObject> allFixedListResult) {

        return FixedListConverterUtil.mapToReferenceDataFixedList(allFixedListResult, FixedList.CREDITOR_NAME.getId().toString());
    }

    @Override
    public void createPrompt(final JsonArrayBuilder promptsPayloadBuilder,
                             final JsonObject terminalEntry,
                             final Prompt prompt,
                             final OffenceDataSupplier offenceDataSupplier) {
        mapProsecutionAuthority(offenceDataSupplier).ifPresent(value -> addPrompt(promptsPayloadBuilder, prompt.getId(), value));
    }
}
