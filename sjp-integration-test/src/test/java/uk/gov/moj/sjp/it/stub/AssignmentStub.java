package uk.gov.moj.sjp.it.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.findAll;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.jayway.awaitility.Awaitility.await;
import static java.lang.String.format;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_ACCEPTED;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;
import uk.gov.moj.cpp.sjp.domain.CaseAssignmentType;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.Response;

import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.json.JSONObject;

public class AssignmentStub {

    private static final String ASSIGNMENTS_QUERY_URL = "/assignment-service/query/api/rest/assignment/assignments";
    private static final String ASSIGNMENTS_QUERY_MEDIA_TYPE = "application/vnd.assignment.query.assignments+json";

    private static final String ASSIGNMENT_COMMAND_URL = "/assignment-service/command/api/rest/assignment/assignments";
    private static final String ADD_ASSIGNMENT_MEDIA_TYPE = "application/vnd.assignment.command.add-assignment-to+json";
    private static final String REMOVE_ASSIGNMENT_MEDIA_TYPE = "application/vnd.assignment.command.remove-assignment+json";

    public static void stubGetEmptyAssignmentsByDomainObjectId(final UUID caseId) {
        stubGetAssignmentsByDomainObjectId(caseId);
    }

    public static void stubGetAssignmentsByDomainObjectId(final UUID caseId, final Optional<String> assignmentNature, final UUID... assignees) {
        InternalEndpointMockUtils.stubPingFor("assignment-service");

        final JsonArrayBuilder assignments = Json.createArrayBuilder();
        for (final UUID assignee : assignees) {
            final JsonObjectBuilder assignment = Json.createObjectBuilder()
                    .add("id", UUID.randomUUID().toString())
                    .add("version", 2)
                    .add("domainObjectId", caseId.toString())
                    .add("assignee", assignee.toString());
            assignmentNature.ifPresent(nature -> assignment.add("assignmentNatureType", nature));
            assignments.add(assignment);
        }

        final JsonObject payload = Json.createObjectBuilder().add("assignments", assignments).build();

        stubFor(get(urlPathEqualTo(ASSIGNMENTS_QUERY_URL))
                .withQueryParam("domainObjectId", equalTo(caseId.toString()))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)
                        .withBody(payload.toString())));

        waitForStubToBeReady(format("%s?%s=%s", ASSIGNMENTS_QUERY_URL, "domainObjectId", caseId), ASSIGNMENTS_QUERY_MEDIA_TYPE);
    }

    public static void stubGetAssignmentsByDomainObjectId(final UUID caseId, final UUID... assignees) {
        stubGetAssignmentsByDomainObjectId(caseId, Optional.empty(), assignees);
    }

    public static void stubAddAssignmentCommand() {
        InternalEndpointMockUtils.stubPingFor("assignment-service");

        stubFor(post(urlPathEqualTo(ASSIGNMENT_COMMAND_URL))
                .withHeader(CONTENT_TYPE, equalTo(ADD_ASSIGNMENT_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void stubRemoveAssignmentCommand() {
        InternalEndpointMockUtils.stubPingFor("assignment-service");

        stubFor(post(urlPathEqualTo(ASSIGNMENT_COMMAND_URL))
                .withHeader(CONTENT_TYPE, equalTo(REMOVE_ASSIGNMENT_MEDIA_TYPE))
                .willReturn(aResponse().withStatus(SC_ACCEPTED)
                        .withHeader(CONTENT_TYPE, APPLICATION_JSON)));
    }

    public static void verifyAddAssignmentCommandSent(final UUID caseId, final UUID assigneeId, final CaseAssignmentType caseAssignmentType) {
        final Predicate<JSONObject> commandPayloadPredicate = commandPayload -> commandPayload.getString("domainObjectId").equals(caseId.toString())
                && commandPayload.getString("assignee").equals(assigneeId.toString())
                && commandPayload.getString("assignmentNatureType").equals(caseAssignmentType.toString());

        verifyCommandSent(ADD_ASSIGNMENT_MEDIA_TYPE, commandPayloadPredicate);
    }

    public static void verifyRemoveAssignmentCommandSend(final UUID caseId) {
        final Predicate<JSONObject> commandPayloadPredicate = commandPayload -> commandPayload.getString("domainObjectId").equals(caseId.toString());

        verifyCommandSent(REMOVE_ASSIGNMENT_MEDIA_TYPE, commandPayloadPredicate);
    }

    private static void verifyCommandSent(final String contentType, final Predicate<JSONObject> payloadPredicate) {
        await().until(() ->
                findAll(postRequestedFor(urlPathEqualTo(ASSIGNMENT_COMMAND_URL))
                        .withHeader(CONTENT_TYPE, equalTo(contentType)))
                        .stream()
                        .map(LoggedRequest::getBodyAsString)
                        .map(JSONObject::new)
                        .anyMatch(payloadPredicate));
    }

}
