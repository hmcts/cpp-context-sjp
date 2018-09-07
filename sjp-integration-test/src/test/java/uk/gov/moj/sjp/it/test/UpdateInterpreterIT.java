package uk.gov.moj.sjp.it.test;


import static javax.json.Json.createObjectBuilder;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.helper.UpdateInterpreterHelper;
import uk.gov.moj.sjp.it.pollingquery.CasePoller;

import java.util.UUID;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class UpdateInterpreterIT extends BaseIntegrationTest {

    private UpdateInterpreterHelper updateInterpreterHelper;
    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void setUp() {
        updateInterpreterHelper = new UpdateInterpreterHelper();
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
    }

    @After
    public void tearDown() throws Exception {
        updateInterpreterHelper.close();
    }

    @Test
    public void shouldUpdateInterpreter() {

        final UUID caseId = createCasePayloadBuilder.getId();
        final String defendantId = CasePoller.pollUntilCaseByIdIsOk(caseId).getString("defendant.id");

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload("french"));

        updateInterpreterHelper.pollForInterpreter(caseId, defendantId, "french");

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload("german"));

        updateInterpreterHelper.pollForInterpreter(caseId, defendantId, "german");

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload(null));

        updateInterpreterHelper.pollForEmptyInterpreter(caseId, defendantId);

        updateInterpreterHelper.updateInterpreter(caseId, defendantId, updateInterpreterPayload(""));

        updateInterpreterHelper.pollForEmptyInterpreter(caseId, defendantId);
    }

    private JsonObject updateInterpreterPayload(final String language) {
        final JsonObjectBuilder objectBuilder = createObjectBuilder();
        if (!StringUtils.isEmpty(language)) {
            objectBuilder.add("language", language);
        }
        return objectBuilder.build();
    }
}
