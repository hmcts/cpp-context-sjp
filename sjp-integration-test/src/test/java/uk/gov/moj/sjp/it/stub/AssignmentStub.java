package uk.gov.moj.sjp.it.stub;


import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.apache.http.HttpStatus.SC_OK;

import java.util.Optional;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class AssignmentStub {

    private static final String ASSIGNMENTS_QUERY_URL = "/assignment-service/query/api/rest/assignment/assignments";

    public static void stubGetEmptyAssignmentsByDomainObjectId(final UUID caseId) {
        stubGetAssignmentsByDomainObjectId(caseId);
    }

    public static void stubGetAssignmentsByDomainObjectId(final UUID caseId, final Optional<String> assignmentNature, final UUID... assignees) {
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
    }

    public static void stubGetAssignmentsByDomainObjectId(final UUID caseId, final UUID... assignees) {
        stubGetAssignmentsByDomainObjectId(caseId, Optional.empty(), assignees);
    }

}
