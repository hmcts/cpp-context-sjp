package uk.gov.moj.sjp.it.helper;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.isJson;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.lang.String.format;
import static javax.json.Json.createObjectBuilder;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.equalTo;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMatcher.jsonEnvelope;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopeMetadataMatcher.metadata;
import static uk.gov.justice.services.test.utils.core.matchers.JsonEnvelopePayloadMatcher.payloadIsJson;
import static uk.gov.moj.sjp.it.util.TopicUtil.retrieveMessageAsJsonObject;

import uk.gov.justice.services.messaging.DefaultJsonObjectEnvelopeConverter;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.sjp.it.util.HttpClientUtil;
import uk.gov.moj.sjp.it.util.TopicUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.Response;

import com.jayway.jsonpath.ReadContext;
import org.apache.commons.lang.RandomStringUtils;
import org.hamcrest.Matcher;

public class EmployerHelper implements AutoCloseable {

    private MessageConsumer messageConsumer;

    public static final String FIELD_ADDRESS_1 = "address1";
    public static final String FIELD_ADDRESS_2 = "address2";
    public static final String FIELD_ADDRESS_3 = "address3";
    public static final String FIELD_ADDRESS_4 = "address4";
    public static final String FIELD_ADDRESS_5 = "address5";
    public static final String FIELD_POST_CODE = "postcode";

    public static final String FIELD_NAME = "name";
    public static final String FIELD_EMPLOYEE_REFERENCE = "employeeReference";
    public static final String FIELD_PHONE = "phone";
    public static final String FIELD_ADDRESS = "address";

    public EmployerHelper() {
        messageConsumer = TopicUtil.publicEvents.createConsumerForMultipleSelectors(
                "public.sjp.employer-updated", "public.sjp.employer-deleted", "public.sjp.case-update-rejected");
    }

    public void updateEmployer(final UUID caseId, final String defendantId, final JsonObject payload) {
        final String resource = String.format("/cases/%s/defendant/%s/employer", caseId, defendantId);
        final String contentType = "application/vnd.sjp.update-employer+json";
        HttpClientUtil.makePostCall(resource, contentType, payload.toString());
    }

    public Response getEmployer(final String defendantId) {
        final String resource = format("/defendant/%s/employer", defendantId);
        final String contentType = "application/vnd.sjp.query.employer+json";
        return HttpClientUtil.makeGetCall(resource, contentType);
    }

    public String getEmployer(final String defendantId, final Matcher<Object> jsonMatcher) {
        return await().atMost(30, TimeUnit.SECONDS).until(() -> getEmployer(defendantId).readEntity(String.class), jsonMatcher);
    }

    public void deleteEmployer(final String caseId, final String defendantId) {
        final String resource = String.format("/cases/%s/defendant/%s/employer", caseId, defendantId);
        final String contentType = "application/vnd.sjp.delete-employer+json";
        HttpClientUtil.makePostCall(resource, contentType, null);
    }

    public JsonEnvelope getEventFromPublicTopic() {
        final String message = retrieveMessageAsJsonObject(messageConsumer).get().toString();
        return new DefaultJsonObjectEnvelopeConverter().asEnvelope(message);
    }


    public static JsonObject getEmployerPayload() {
        final JsonObject address = createObjectBuilder()
                .add(FIELD_ADDRESS_1, RandomStringUtils.randomAlphabetic(12))
                .add(FIELD_ADDRESS_2, "Flat 8")
                .add(FIELD_ADDRESS_3, "Lant House")
                .add(FIELD_ADDRESS_4, "London")
                .add(FIELD_ADDRESS_5, "Greater London")
                .add(FIELD_POST_CODE, "SE1 1PJ").build();
        return createObjectBuilder()
                .add(FIELD_NAME, RandomStringUtils.randomAlphabetic(12))
                .add(FIELD_EMPLOYEE_REFERENCE, "abcdef")
                .add(FIELD_PHONE, "02020202020")
                .add(FIELD_ADDRESS, address).build();
    }

    public static Matcher<ReadContext> getEmployerUpdatedPayloadContentMatcher(final JsonObject employer) {

        final JsonObject address = employer.getJsonObject(FIELD_ADDRESS);
        return allOf(
                withJsonPath("$.name", equalTo(employer.getString(FIELD_NAME))),
                withJsonPath("$.employeeReference", equalTo(employer.getString(FIELD_EMPLOYEE_REFERENCE))),
                withJsonPath("$.phone", equalTo(employer.getString(FIELD_PHONE))),
                withJsonPath("$.address.address1", equalTo(address.getString(FIELD_ADDRESS_1))),
                withJsonPath("$.address.address2", equalTo(address.getString(FIELD_ADDRESS_2))),
                withJsonPath("$.address.address3", equalTo(address.getString(FIELD_ADDRESS_3))),
                withJsonPath("$.address.address4", equalTo(address.getString(FIELD_ADDRESS_4))),
                withJsonPath("$.address.address5", equalTo(address.getString(FIELD_ADDRESS_5))),
                withJsonPath("$.address.postcode", equalTo(address.getString(FIELD_POST_CODE)))
        );
    }

    public static Matcher<Object> getEmployerUpdatedPayloadMatcher(final JsonObject employer) {
        return isJson(getEmployerUpdatedPayloadContentMatcher(employer));
    }

    public static Matcher<JsonEnvelope> getEmployerUpdatedPublicEventMatcher(final JsonObject employer) {
        final Matcher<ReadContext> payloadContentMatcher = getEmployerUpdatedPayloadContentMatcher(employer);
        return jsonEnvelope()
                .withMetadataOf(metadata().withName("public.sjp.employer-updated"))
                .withPayloadOf(payloadIsJson(payloadContentMatcher));
    }

    public static Matcher<JsonEnvelope> getEmployerDeletedPublicEventMatcher(final String defendantId) {
        return jsonEnvelope()
                .withMetadataOf(metadata().withName("public.sjp.employer-deleted"))
                .withPayloadOf(payloadIsJson(withJsonPath("$.defendantId", equalTo(defendantId))));
    }

    @Override
    public void close() throws Exception {
        messageConsumer.close();
    }
}
