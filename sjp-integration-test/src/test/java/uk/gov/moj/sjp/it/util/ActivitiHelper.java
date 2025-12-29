package uk.gov.moj.sjp.it.util;

import static java.util.Optional.empty;
import static java.util.concurrent.TimeUnit.SECONDS;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.POLL_INTERVAL;
import static uk.gov.moj.sjp.it.util.RestPollerWithDefaults.TIMEOUT_IN_SECONDS;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory;

import java.io.StringReader;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import uk.gov.justice.services.messaging.JsonObjects;
import javax.json.JsonObject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import io.restassured.path.json.JsonPath;

public class ActivitiHelper {

    private final static String ACTIVITI_BASE_PATH = getBaseUri() + "/sjp-event-processor/internal/activiti/service/";

    private static final UUID USER_ID = UUID.randomUUID();

    public static Optional<String> getProcessesInstanceIds(final String processName, final String businessKey) {
        final Response processes = ResteasyClientBuilderFactory.clientBuilder().build()
                .target(ACTIVITI_BASE_PATH + "runtime/process-instances?processDefinitionKey=" + processName + "&businessKey=" + businessKey)
                .request()
                .headers(headers())
                .get();

        return new JsonPath(processes.readEntity(String.class))
                .getList("data.id", String.class)
                .stream()
                .findFirst();
    }

    public static Optional<String> getAocpProcessesInstanceIds(final String processName, final String businessKey) {
        final Response processes = ResteasyClientBuilderFactory.clientBuilder().build()
                .target(ACTIVITI_BASE_PATH + "runtime/process-instances?processDefinitionKey=" + processName + "&businessKey=" + businessKey)
                .request()
                .headers(headers())
                .get();

        final List<String> processList = new JsonPath(processes.readEntity(String.class))
                .getList("data.id", String.class);

        return IntStream.range(0, processList.size())
                .filter(n -> n % 2 == 1)
                .mapToObj(processList::get)
                .findFirst();
    }

    public static String pollUntilProcessExists(final String processName, final String businessKey) {
        return await().pollInterval(POLL_INTERVAL).atMost(TIMEOUT_IN_SECONDS, SECONDS).until(() -> getProcessesInstanceIds(processName, businessKey), not(equalTo(empty()))).get();
    }

    public static String pollUntilAocpProcessExists(final String processName, final String businessKey) {
        return await().pollInterval(POLL_INTERVAL).atMost(TIMEOUT_IN_SECONDS, SECONDS).until(() -> getAocpProcessesInstanceIds(processName, businessKey), not(equalTo(empty()))).get();
    }

    public static void executeTimerJobs(final String processInstanceId) {
        getTimerJobs(processInstanceId)
                .getJsonArray("data")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(job -> job.getString("id"))
                .forEach(ActivitiHelper::executeJob);
    }

    private static MultivaluedMap<String, Object> headers() {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HeaderConstants.USER_ID, USER_ID.toString());
        return headers;
    }

    private static JsonObject runQuery(final String url) {
        final RestClient restClient = new RestClient();
        final Response response = restClient.query(url, APPLICATION_JSON);
        return createReader(new StringReader(response.readEntity(String.class))).readObject();
    }

    private static JsonObject getTimerJobs(final String processInstanceId) {
        final String url = ACTIVITI_BASE_PATH + "management/jobs?timersOnly=true&processInstanceId=" + processInstanceId;
        return runQuery(url);
    }

    private static void executeJob(final String jobId) {
        final String url = ACTIVITI_BASE_PATH + "management/jobs/" + jobId;
        sendPostRequest(url, JsonObjects.createObjectBuilder().add("action", "execute").build());
    }

    private static Response sendPostRequest(final String url, final JsonObject payload) {
        final Entity<String> entity = Entity.entity(payload.toString(), MediaType.valueOf(APPLICATION_JSON));
        return ResteasyClientBuilderFactory.clientBuilder().build().target(url).request().post(entity);
    }

}