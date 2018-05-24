package uk.gov.moj.sjp.it.test;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.GUILTY;
import static uk.gov.moj.cpp.sjp.domain.plea.PleaType.NOT_GUILTY;

import uk.gov.moj.cpp.sjp.domain.plea.PleaType;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.CancelPleaHelper;
import uk.gov.moj.sjp.it.helper.UpdatePleaHelper;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.junit.Before;
import org.junit.Test;

public class UpdatePleaInterpreterIT extends BaseIntegrationTest {

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
    }

    @Test
    public void shouldAddUpdateAndCancelPleaAndInterpreterLanguage() {
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper();
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId())) {

            String language = "Swahili";
            final JsonObject addPleaRequest = getPleaPayload(NOT_GUILTY, true, language);
            updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), addPleaRequest);
            updatePleaHelper.verifyInterpreterLanguage(createCasePayloadBuilder.getId(), language);

            language = "Elvish";
            final JsonObject updateInterpreterRequest = getPleaPayload(NOT_GUILTY, true, language);
            updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), updateInterpreterRequest);
            updatePleaHelper.verifyInterpreterLanguage(createCasePayloadBuilder.getId(), language);

            language = null;
            final JsonObject cancelInterpreterRequest = getPleaPayload(NOT_GUILTY, false, language);
            updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), cancelInterpreterRequest);
            cancelPleaHelper.verifyInterpreterCancelled();

            language = "Hindi";
            final JsonObject addInterpreterRequest = getPleaPayload(NOT_GUILTY, true, language);
            updatePleaHelper.updatePlea(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), addInterpreterRequest);
            updatePleaHelper.verifyInterpreterLanguage(createCasePayloadBuilder.getId(), language);

            cancelPleaHelper.cancelPlea();
            cancelPleaHelper.verifyInterpreterCancelled();
        }
    }

    // We could also test other invalid bad requests but these should already be covered by unit tests
    @Test
    public void shouldRejectInvalidInterpreterRequest() {
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper()) {
            // Interpreter fields shouldn't be set for GUILTY pleas
            final JsonObject addPleaRequest = getPleaPayload(GUILTY, true, "Swedish");
            updatePleaHelper.updatePleaAndExpectBadRequest(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId(), addPleaRequest);
        }
    }

    public static JsonObject getPleaPayload(final PleaType pleaType, final Boolean interpreterRequired, final String interpreterLanguage) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add("plea", pleaType.name());
        if (interpreterRequired != null) {
            builder.add("interpreterRequired", interpreterRequired);
        }
        if (interpreterLanguage != null) {
            builder.add("interpreterLanguage", interpreterLanguage);
        }
        return builder.build();
    }

}
