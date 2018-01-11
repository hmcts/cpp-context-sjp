package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import uk.gov.moj.sjp.it.helper.CaseSjpHelper;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.Optional;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

//TODO public events
public class SjpCaseCreationFailedBecauseCaseAlreadyExistedIT extends BaseIntegrationTest{

    private MessageConsumer sjpCaseCreated =
            QueueUtil.publicEvents.createConsumer("public.structure.sjp-case-created");

    private MessageConsumer caseCreationFailedBecauseCaseAlreadyExisted =
            QueueUtil.publicEvents.createConsumer(
                    "public.structure.case-creation-failed-because-case-already-existed");

    private CaseSjpHelper caseSjpHelper;

    @Before
    public void createACaseAfterAnother() {
        caseSjpHelper = new CaseSjpHelper();
        caseSjpHelper.createCase();
        caseSjpHelper.createCase();
    }

    @After
    public void tearDown() {
        caseSjpHelper.close();
    }

    @Test
    public void publishesCaseCreationFailedBecauseCaseAlreadyExisted() throws Exception {
        Optional<JsonObject> message1 = QueueUtil.retrieveMessageAsJsonObject(sjpCaseCreated);
        assertTrue(message1.isPresent());
        assertThat(message1.get(), isJson(withJsonPath("$.id", Matchers.hasToString(
                Matchers.containsString(caseSjpHelper.getCaseId())))
        ));

        Optional<JsonObject> message2 = QueueUtil.retrieveMessageAsJsonObject(caseCreationFailedBecauseCaseAlreadyExisted);
        assertTrue(message2.isPresent());
        assertThat(message2.get(), isJson(withJsonPath("$.caseId", Matchers.hasToString(
                Matchers.containsString(caseSjpHelper.getCaseId())))
        ));
    }
}
