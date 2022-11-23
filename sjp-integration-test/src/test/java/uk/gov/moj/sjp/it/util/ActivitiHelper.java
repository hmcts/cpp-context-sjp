package uk.gov.moj.sjp.it.util;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.jsonpath.matchers.JsonPathMatchers.withJsonPath;
import static java.util.Optional.empty;
import static javax.json.Json.createReader;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.not;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;
import static uk.gov.justice.services.test.utils.core.matchers.JsonValueIsJsonMatcher.isJson;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.rest.RestClient;
import uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory;

import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.jayway.awaitility.core.ConditionTimeoutException;
import com.jayway.restassured.path.json.JsonPath;
import org.hamcrest.Matcher;

public class ActivitiHelper {

    private final static String ACTIVITI_BASE_PATH = getBaseUri() + "/sjp-event-processor/internal/activiti/service/";

    private static final UUID USER_ID = UUID.randomUUID();

    public static boolean processExists(final String businessKey, final String processName) {
        final Matcher<JsonValue> existsMatcher = isJson(withJsonPath("$.data", not(empty())));
        try {
            pollForProcess(businessKey, processName, existsMatcher);
        } catch (final ConditionTimeoutException e) {
            return false;
        }
        return true;
    }

    public static boolean isProcessDeleted(final String businessKey, final String processName, final String reason) {
        final Matcher<JsonValue> deletedMatcher = isJson(withJsonPath("$.data.[0].deleteReason", equalTo(reason)));
        try {
            pollForProcessHistory(businessKey, processName, deletedMatcher);
        } catch (final ConditionTimeoutException e) {
            return false;
        }
        return true;
    }

    public static void pollUntilProcessDeleted(final String processName, final String businessKey, final String reason) {
        await().until(() -> isProcessDeleted(businessKey, processName, reason), is(true));
    }

    public static String createProcessInstance(final String processName, final String businessKey, final Map<String, Object> parameters) {
        final JsonObject createProcessPayload = Json.createObjectBuilder()
                .add("processDefinitionKey", processName)
                .add("businessKey", businessKey)
                .add("variables", parametersMapToJsonArray(parameters))
                .build();

        final Response createProcessInstanceResponse = sendPostRequest(
                ACTIVITI_BASE_PATH + "runtime/process-instances", createProcessPayload);

        return new JsonPath(createProcessInstanceResponse.readEntity(String.class)).getString("id");
    }

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
        return await().until(() -> getProcessesInstanceIds(processName, businessKey), not(equalTo(empty()))).get();
    }

    public static String pollUntilAocpProcessExists(final String processName, final String businessKey) {
        return await().until(() -> getAocpProcessesInstanceIds(processName, businessKey), not(equalTo(empty()))).get();
    }

    public static void pollUntilProcessDoesNotExist(final String processName, final String businessKey) {
        await().until(() -> getProcessesInstanceIds(processName, businessKey), equalTo(empty()));
    }

    public static void signalProcessesInstanceId(final String processInstanceId, final String signalName, final Map<String, Object> parameters) {
        final Response executionResponse = ResteasyClientBuilderFactory.clientBuilder().build()
                .target(ACTIVITI_BASE_PATH + "runtime/executions?processInstanceId=" + processInstanceId + "&signalEventSubscriptionName=" + signalName)
                .request()
                .headers(headers())
                .get();

        final String executionId = new JsonPath(executionResponse.readEntity(String.class))
                .getList("data.id", String.class)
                .stream()
                .findFirst().get();

        final JsonObject signalPayload = Json.createObjectBuilder()
                .add("action", "signalEventReceived")
                .add("signalName", signalName)
                .add("variables", parametersMapToJsonArray(parameters))
                .build();

        ResteasyClientBuilderFactory.clientBuilder().build()
                .target(ACTIVITI_BASE_PATH + "runtime/executions/" + executionId)
                .request()
                .headers(headers())
                .put(entity(signalPayload.toString(), APPLICATION_JSON));
    }

    public static void deleteProcessInstance(final String id) {
        ResteasyClientBuilderFactory.clientBuilder().build()
                .target(ACTIVITI_BASE_PATH + "runtime/process-instances/" + id)
                .request()
                .headers(headers())
                .delete();
    }

    public static void executeTimerJobs(final String processInstanceId) {
        getTimerJobs(processInstanceId)
                .getJsonArray("data")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(job -> job.getString("id"))
                .forEach(ActivitiHelper::executeJob);
    }

    private static JsonArray parametersMapToJsonArray(final Map<String, Object> parameters) {
        final JsonArrayBuilder arrayOfParameters = Json.createArrayBuilder();
        parameters.entrySet().stream()
                .map(param -> Json.createObjectBuilder().add("name", param.getKey()).add("value", param.getValue().toString()))
                .forEach(arrayOfParameters::add);
        return arrayOfParameters.build();
    }

    private static MultivaluedMap<String, Object> headers() {
        final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add(HeaderConstants.USER_ID, USER_ID.toString());
        return headers;
    }

    private static JsonObject pollForProcess(final String businessKey, final String processName, final Matcher<JsonValue> responseMatcher) {
        final JsonObject body = await().until(() -> getProcesses(businessKey, processName), responseMatcher);
        return createReader(new StringReader(body.toString())).readObject().getJsonArray("data").getJsonObject(0);
    }

    private static JsonObject pollForProcessHistory(final String businessKey, final String processName, final Matcher<JsonValue> responseMatcher) {
        final JsonObject body = await().until(() -> getProcessesHistory(businessKey, processName), responseMatcher);
        return createReader(new StringReader(body.toString())).readObject();
    }

    private static JsonObject getProcessesHistory(final String businessKey, final String processName) {
        final String url = ACTIVITI_BASE_PATH + "history/historic-process-instances?businessKey=" + businessKey + "&processDefinitionKey=" + processName;
        return runQuery(url);
    }

    private static JsonObject getProcesses(final String businessKey, final String processName) {
        final String url = ACTIVITI_BASE_PATH + "runtime/process-instances?businessKey=" + businessKey + "&processDefinitionKey=" + processName;
        return runQuery(url);
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
        sendPostRequest(url, Json.createObjectBuilder().add("action", "execute").build());
    }

    private static Response sendPostRequest(final String url, final JsonObject payload) {
        final Entity<String> entity = Entity.entity(payload.toString(), MediaType.valueOf(APPLICATION_JSON));
        return ResteasyClientBuilderFactory.clientBuilder().build().target(url).request().post(entity);
    }

}