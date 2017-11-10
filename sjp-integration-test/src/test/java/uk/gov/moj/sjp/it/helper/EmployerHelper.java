package uk.gov.moj.sjp.it.helper;

import static com.jayway.awaitility.Awaitility.await;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.common.http.HeaderConstants.USER_ID;
import static uk.gov.moj.sjp.it.helper.AbstractTestHelper.getReadUrl;
import static uk.gov.moj.sjp.it.helper.AbstractTestHelper.getWriteUrl;
import static uk.gov.moj.sjp.it.util.QueueUtil.retrieveMessageAsJsonObject;

import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.moj.sjp.it.util.QueueUtil;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.jms.MessageConsumer;
import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.hamcrest.Matcher;

public class EmployerHelper implements AutoCloseable {

    private final MultivaluedMap<String, Object> headers;
    private final RestClient restClient;
    private MessageConsumer messageConsumer;

    public EmployerHelper() {
        restClient = new RestClient();
        headers = new MultivaluedHashMap<>();
        headers.add(USER_ID, UUID.randomUUID());
        messageConsumer = QueueUtil.publicEvents.createConsumerForMultipleSelectors(
                "public.structure.employer-updated", "public.structure.case-update-rejected");
    }

    public Response updateEmployer(final String caseId, final String defendantId, final JsonObject payload) {
        final String resource = getWriteUrl(String.format("/cases/%s/defendant/%s/employer", caseId, defendantId));
        final String contentType = "application/vnd.sjp.update-employer+json";
        return restClient.postCommand(resource, contentType, payload.toString(), headers);
    }

    public Response getEmployer(final String defendantId) {
        final String resource = getReadUrl(format("/defendant/%s/employer", defendantId));
        final String contentType = "application/vnd.sjp.query.employer+json";
        return restClient.query(resource, contentType, headers);
    }

    public String getEmployer(final String defendantId, final Matcher jsonMatcher) {
        return await().atMost(20, TimeUnit.SECONDS).until(() -> getEmployer(defendantId).readEntity(String.class), jsonMatcher);
    }

    public String getEventFromPublicTopic() {
        return retrieveMessageAsJsonObject(messageConsumer).get().toString();
    }

    @Override
    public void close() throws Exception {
        messageConsumer.close();
    }
}
