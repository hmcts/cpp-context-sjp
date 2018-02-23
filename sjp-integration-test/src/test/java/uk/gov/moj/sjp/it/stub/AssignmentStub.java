package uk.gov.moj.sjp.it.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static java.lang.String.format;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;
import static uk.gov.moj.sjp.it.util.WiremockTestHelper.waitForStubToBeReady;

import uk.gov.justice.service.wiremock.testutil.InternalEndpointMockUtils;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class AssignmentStub {

    private static final String ASSIGNMENTS_QUERY_URL = "/assignment-query-api/query/api/rest/assignment/assignments";
    private static final String ASSIGNMENTS_QUERY_MEDIA_TYPE = "application/vnd.assignment.query.assignments+json";

    public static void stubGetEmptyAssignmentsByDomainObjectId(final String caseId) {
        stubGetAssignmentsByDomainObjectId(caseId);
    }

    public static void stubGetAssignmentsByDomainObjectId(final String caseId, final Optional<String> assignmentNature, final UUID... assignees) {
        InternalEndpointMockUtils.stubPingFor("assignment-query-api");

        final JsonArrayBuilder assignments = Json.createArrayBuilder();
        for (final UUID assignee : assignees) {
            final JsonObjectBuilder assignment = Json.createObjectBuilder()
                    .add("id", UUID.randomUUID().toString())
                    .add("version", 2)
                    .add("domainObjectId", caseId)
                    .add("assignee", assignee.toString());
            assignmentNature.ifPresent(nature -> assignment.add("assignmentNatureType", nature));
            assignments.add(assignment);
        }

        final JsonObject payload = Json.createObjectBuilder().add("assignments", assignments).build();

        stubFor(get(urlPathEqualTo(ASSIGNMENTS_QUERY_URL))
                .withQueryParam("domainObjectId", equalTo(caseId))
                .willReturn(aResponse().withStatus(SC_OK)
                        .withHeader("CPPID", UUID.randomUUID().toString())
                        .withHeader("Content-Type", APPLICATION_JSON)
                        .withBody(payload.toString())));

        waitForStubToBeReady(format("%s?%s=%s", ASSIGNMENTS_QUERY_URL, "domainObjectId", caseId), ASSIGNMENTS_QUERY_MEDIA_TYPE);
    }

    public static void stubGetAssignmentsByDomainObjectId(final String caseId, final UUID... assignees) {
        stubGetAssignmentsByDomainObjectId(caseId, Optional.empty(), assignees);
    }

}
