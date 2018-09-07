package uk.gov.moj.sjp.it.test;


import static uk.gov.moj.sjp.it.helper.UpdateInterpreterHelper.buildUpdateInterpreterPayload;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.UpdateInterpreterHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class UpdatePleaInterpreterIT extends BaseIntegrationTest {

    private static UpdateInterpreterHelper updateInterpreterHelper;

    private UUID caseId;
    private String defendantId;

    @BeforeClass
    public static void openListener() {
        updateInterpreterHelper = new UpdateInterpreterHelper();
    }

    @AfterClass
    public static void closeListener() throws Exception {
        updateInterpreterHelper.close();
    }

    @Before
    public void setUpNewCase() {
        final CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(createCasePayloadBuilder);

        caseId = createCasePayloadBuilder.getId();
        defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");
    }

    @Test
    public void shouldUpdateAndCancelInterpreter() {
        updateInterpreter("spanish");

        updateInterpreterHelper.pollForInterpreter(caseId, defendantId, "spanish");

        updateInterpreter(null);

        updateInterpreterHelper.pollForEmptyInterpreter(caseId, defendantId);
    }

    private void updateInterpreter(final String interpreterLanguage) {
        updateInterpreterHelper.updateInterpreter(caseId, defendantId,
                buildUpdateInterpreterPayload(interpreterLanguage));
    }

}
