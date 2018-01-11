package uk.gov.moj.sjp.it.helper;

import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.moj.sjp.it.EventSelector.EVENT_SELECTOR_ADD_PERSON_INFO;
import static uk.gov.moj.sjp.it.EventSelector.PUBLIC_EVENT_SELECTOR_ADD_PERSON_INFO;
import static uk.gov.moj.sjp.it.util.QueueUtil.privateEvents;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessage;

import uk.gov.justice.services.test.utils.core.messaging.MessageConsumerClient;

import java.time.LocalDate;

import javax.json.JsonObject;

import com.jayway.restassured.path.json.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddPersonInfoHelper extends AbstractTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AddPersonInfoHelper.class);

    private static final String WRITE_MEDIA_TYPE = "application/vnd.sjp.add-person-info+json";

    private final MessageConsumerClient privateEventAddPersonInfoFailedConsumer = new MessageConsumerClient();

    private final CaseSjpHelper caseSjpHelper;

    private String request;

    public AddPersonInfoHelper(final CaseSjpHelper caseSjpHelper) {
        privateEventsConsumer = privateEvents.createConsumer(EVENT_SELECTOR_ADD_PERSON_INFO);
        privateEventAddPersonInfoFailedConsumer.startConsumer(PUBLIC_EVENT_SELECTOR_ADD_PERSON_INFO, SJP_EVENT_TOPIC);
        publicConsumer.startConsumer(PUBLIC_EVENT_SELECTOR_ADD_PERSON_INFO, PUBLIC_ACTIVE_MQ_TOPIC);
        this.caseSjpHelper = caseSjpHelper;
    }

    public void addPersonInfo(final String mappingId) {
        final JsonObject personInfoAsJsonObject = createObjectBuilder()
                .add("id", mappingId)
                .add("personId", caseSjpHelper.getDefendantPersonId())
                .add("firstName", "firstName")
                .add("lastName", "lastName")
                .add("dateOfBirth", LocalDate.now().toString())
                .add("postCode", "postCode")
                .build();

        request = personInfoAsJsonObject.toString();

        makePostCall(getWriteUrl("/cases/" + caseSjpHelper.getCaseId() + "/add-person-info"), WRITE_MEDIA_TYPE, request);
    }

    public void verifyInActiveMQ() {
        JsonPath jsRequest = new JsonPath(request);
        LOGGER.info("Request payload: {}", jsRequest.prettify());

        JsonPath jsonResponse = retrieveMessage(privateEventsConsumer);

        assertThat(jsRequest.getString("id"), is(jsonResponse.get("id").toString()));
        assertThat(jsRequest.getString("personId"), is(jsonResponse.get("personId").toString()));
        assertThat(jsRequest.getString("firstName"), is(jsonResponse.get("firstName").toString()));
        assertThat(jsRequest.getString("lastName"), is(jsonResponse.get("lastName").toString()));
        assertThat(LocalDate.now().toString(), is(jsonResponse.get("dateOfBirth").toString()));
        assertThat(jsRequest.getString("postCode"), is(jsonResponse.get("postCode").toString()));

    }

    public void verifyPersonInfoAdded(final int expectedCount) {
        try (final CaseSearchResultHelper caseSearchResultHelper = new CaseSearchResultHelper(caseSjpHelper)) {
            caseSearchResultHelper.verifyPersonInfoByUrn(expectedCount);
        }
    }

    @Override
    public void close() {
        super.close();
        privateEventAddPersonInfoFailedConsumer.close();
        publicConsumer.close();
        privateEventAddPersonInfoFailedConsumer.close();
    }
}
