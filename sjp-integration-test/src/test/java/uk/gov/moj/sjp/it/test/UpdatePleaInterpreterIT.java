package uk.gov.moj.sjp.it.test;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.moj.sjp.it.stub.AssignmentStub.stubGetEmptyAssignmentsByDomainObjectId;
import static uk.gov.moj.sjp.it.stub.ResultingStub.stubGetCaseDecisionsWithNoDecision;
import static uk.gov.moj.sjp.it.test.UpdatePleaIT.PLEA_GUILTY;
import static uk.gov.moj.sjp.it.test.UpdatePleaIT.PLEA_NOT_GUILTY;

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
        stubGetCaseDecisionsWithNoDecision(createCasePayloadBuilder.getId());
        stubGetEmptyAssignmentsByDomainObjectId(createCasePayloadBuilder.getId());
    }

    @Test
    public void shouldAddUpdateAndCancelPleaAndInterpreterLanguage() {
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId());
             final CancelPleaHelper cancelPleaHelper = new CancelPleaHelper(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId())) {

            String language = "Swahili";
            final JsonObject addPleaRequest = getPleaPayload(PLEA_NOT_GUILTY, true, language);
            updatePleaHelper.updatePlea(addPleaRequest);
            updatePleaHelper.verifyInterpreterLanguage(language);

            language = "Elvish";
            final JsonObject updateInterpreterRequest = getPleaPayload(PLEA_NOT_GUILTY, true, language);
            updatePleaHelper.updatePlea(updateInterpreterRequest);
            updatePleaHelper.verifyInterpreterLanguage(language);

            language = null;
            final JsonObject cancelInterpreterRequest = getPleaPayload(PLEA_NOT_GUILTY, false, language);
            updatePleaHelper.updatePlea(cancelInterpreterRequest);
            cancelPleaHelper.verifyInterpreterCancelled();

            language = "Hindi";
            final JsonObject addInterpreterRequest = getPleaPayload(PLEA_NOT_GUILTY, true, language);
            updatePleaHelper.updatePlea(addInterpreterRequest);
            updatePleaHelper.verifyInterpreterLanguage(language);

            cancelPleaHelper.cancelPlea();
            cancelPleaHelper.verifyInterpreterCancelled();
        }
    }

    // We could also test other invalid bad requests but these should already be covered by unit tests
    @Test
    public void shouldRejectInvalidInterpreterRequest() {
        try (final UpdatePleaHelper updatePleaHelper = new UpdatePleaHelper(createCasePayloadBuilder.getId(), createCasePayloadBuilder.getOffenceId())) {
            // Interpreter fields shouldn't be set for GUILTY pleas
            final JsonObject addPleaRequest = getPleaPayload(PLEA_GUILTY, true, "Swedish");
            updatePleaHelper.updatePleaAndExpectBadRequest(addPleaRequest);
        }
    }

    public static JsonObject getPleaPayload(final String plea, final Boolean interpreterRequired, final String interpreterLanguage) {
        final JsonObjectBuilder builder = createObjectBuilder()
                .add("plea", plea);
        if (interpreterRequired != null) {
            builder.add("interpreterRequired", interpreterRequired);
        }
        if (interpreterLanguage != null) {
            builder.add("interpreterLanguage", interpreterLanguage);
        }
        return builder.build();
    }

}
