package uk.gov.moj.sjp.it.util;

import static com.jayway.awaitility.Awaitility.await;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.not;
import static uk.gov.justice.services.test.utils.core.http.BaseUriProvider.getBaseUri;

import uk.gov.justice.services.common.http.HeaderConstants;
import uk.gov.justice.services.test.utils.core.rest.ResteasyClientBuilderFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.jayway.restassured.path.json.JsonPath;

public class ActivitiHelper {

    private final static String ACTIVITI_BASE_PATH = getBaseUri() + "/sjp-event-processor/internal/activiti/service/";

    private static final UUID USER_ID = UUID.randomUUID();

    public static String createProcessInstance(final String processName, final String businessKey, final Map<String, Object> parameters) {
        final JsonObject createProcessPayload = Json.createObjectBuilder()
                .add("processDefinitionKey", processName)
                .add("businessKey", businessKey)
                .add("variables", parametersMapToJsonArray(parameters))
                .build();

        final Response createProcessInstanceResponse = ResteasyClientBuilderFactory.clientBuilder().build()
                .target(ACTIVITI_BASE_PATH + "runtime/process-instances")
                .request()
                .headers(headers())
                .post(entity(createProcessPayload.toString(), APPLICATION_JSON));

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

    public static String pollUntilProcessExists(final String processName, final String businessKey) {
        return await().until(() -> getProcessesInstanceIds(processName, businessKey), not(equalTo(Optional.empty()))).get();
    }

    public static void pollUntilProcessDoesNotExist(final String processName, final String businessKey) {
        await().until(() -> getProcessesInstanceIds(processName, businessKey), equalTo(Optional.empty()));
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
}