package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

//TODO public events
public class SjpCaseCreationFailedBecauseCaseAlreadyExistedIT extends BaseIntegrationTest{

    private MessageConsumer sjpCaseCreated =
            QueueUtil.publicEvents.createConsumer("public.sjp.sjp-case-created");

    private MessageConsumer caseCreationFailedBecauseCaseAlreadyExisted =
            QueueUtil.publicEvents.createConsumer(
                    "public.sjp.case-creation-failed-because-case-already-existed");

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void createACaseAfterAnother() {
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
    }

    @Test
    public void publishesCaseCreationFailedBecauseCaseAlreadyExisted() throws Exception {
        Optional<JsonObject> message1 = QueueUtil.retrieveMessageAsJsonObject(sjpCaseCreated);
        assertTrue(message1.isPresent());
        assertThat(message1.get(), isJson(withJsonPath("$.id", Matchers.hasToString(
                Matchers.containsString(createCasePayloadBuilder.getId().toString())))
        ));

        Optional<JsonObject> message2 = QueueUtil.retrieveMessageAsJsonObject(caseCreationFailedBecauseCaseAlreadyExisted);
        assertTrue(message2.isPresent());
        assertThat(message2.get(), isJson(withJsonPath("$.caseId", Matchers.hasToString(
                Matchers.containsString(createCasePayloadBuilder.getId().toString())))
        ));
    }
}
