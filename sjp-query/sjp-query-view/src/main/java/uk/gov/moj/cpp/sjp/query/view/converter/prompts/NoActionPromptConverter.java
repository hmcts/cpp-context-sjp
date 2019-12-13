package uk.gov.moj.cpp.sjp.query.view.converter.prompts;

import uk.gov.moj.cpp.sjp.query.view.converter.Prompt;
import uk.gov.moj.cpp.sjp.query.view.service.OffenceDataSupplier;

import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

public class NoActionPromptConverter extends PromptConverter {

    @Override
    public void createPrompt(final JsonArrayBuilder promptsPayloadBuilder, final JsonObject terminalEntry, Prompt prompt, final OffenceDataSupplier offenceDataSupplier) {
            /*
              Way we calculate prompt value is using two separate terminal entries, which we are processing at resultCode level.
              that is why this method is overridden to not provide any implementation logic here.
             */
    }
}
