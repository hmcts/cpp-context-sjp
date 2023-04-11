package uk.gov.moj.sjp.it.test;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.UUID.randomUUID;
import static org.junit.Assert.assertNotNull;
import static uk.gov.moj.sjp.it.model.ProsecutingAuthority.TFL;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubEnforcementAreaByPostcode;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubProsecutorQuery;
import static uk.gov.moj.sjp.it.stub.ReferenceDataServiceStub.stubRegionByPostcode;


import com.jayway.restassured.path.json.JsonPath;
import javax.jms.JMSException;
import org.junit.After;
import uk.gov.moj.sjp.it.command.CreateCase;
import uk.gov.moj.sjp.it.model.ProsecutingAuthority;
import uk.gov.moj.sjp.it.util.TopicUtil;

import javax.jms.MessageConsumer;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

//TODO public events
public class SjpCaseCreationFailedBecauseCaseAlreadyExistedIT extends BaseIntegrationTest{

    private MessageConsumer sjpCaseCreated ;

    private MessageConsumer caseCreationFailedBecauseCaseAlreadyExisted;

    private CreateCase.CreateCasePayloadBuilder createCasePayloadBuilder;

    @Before
    public void createACaseAfterAnother() {
        this.createCasePayloadBuilder = CreateCase.CreateCasePayloadBuilder.withDefaults();
        stubEnforcementAreaByPostcode(createCasePayloadBuilder.getDefendantBuilder().getAddressBuilder().getPostcode(), "1080", "Bedfordshire Magistrates' Court");
        stubRegionByPostcode("1080", "TestRegion");
        sjpCaseCreated =
                TopicUtil.publicEvents.createConsumer("public.sjp.sjp-case-created");
        caseCreationFailedBecauseCaseAlreadyExisted =
                TopicUtil.publicEvents.createConsumer(
                        "public.sjp.case-creation-failed-because-case-already-existed");
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
        CreateCase.createCaseForPayloadBuilder(this.createCasePayloadBuilder);
    }

    @After
    public void after() throws JMSException {
        sjpCaseCreated.close();
        caseCreationFailedBecauseCaseAlreadyExisted.close();
    }

    @Test
    public void publishesCaseCreationFailedBecauseCaseAlreadyExisted() {
        final ProsecutingAuthority prosecutingAuthority = TFL;
        stubProsecutorQuery(prosecutingAuthority.name(), prosecutingAuthority.getFullName(), randomUUID());
        JsonPath message1 = TopicUtil.retrieveMessage(sjpCaseCreated, isJson(withJsonPath("$.id", Matchers.hasToString(
                Matchers.containsString(createCasePayloadBuilder.getId().toString())))
        ));
        assertNotNull(message1);
        JsonPath message2 = TopicUtil.retrieveMessage(caseCreationFailedBecauseCaseAlreadyExisted, isJson(withJsonPath("$.caseId", Matchers.hasToString(
                Matchers.containsString(createCasePayloadBuilder.getId().toString())))
        ));
        assertNotNull(message2);
    }
}
