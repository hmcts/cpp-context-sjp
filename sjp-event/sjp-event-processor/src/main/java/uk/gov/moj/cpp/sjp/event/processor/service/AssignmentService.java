package uk.gov.moj.cpp.sjp.event.processor.service;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.SessionType;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class AssignmentService {

    @Inject
    private CaseAssignmentConfiguration caseAssignmentConfiguration;

    @Inject
    private Enveloper enveloper;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Requester requester;

    public List<AssignmentCandidate> getAssignmentCandidates(final JsonEnvelope envelope, final UUID legalAdviserId, final String courtCode, final SessionType sessionType) {

        final int assignmentCandidatesLimit = caseAssignmentConfiguration.getAssignmentCandidatesLimit();

        final String excludedProsecutingAuthorities = caseAssignmentConfiguration.getProsecutingAuthoritiesAssignmentRules()
                .getCourtExcludedProsecutingAuthorities(courtCode)
                .stream()
                .collect(joining(","));

        final JsonObject queryOptions = createObjectBuilder()
                .add("sessionType", sessionType.name())
                .add("assigneeId", legalAdviserId.toString())
                .add("excludedProsecutingAuthorities", excludedProsecutingAuthorities)
                .add("limit", assignmentCandidatesLimit)
                .build();

        return requester.request(enveloper.withMetadataFrom(envelope, "sjp.query.assignment-candidates")
                .apply(queryOptions))
                .payloadAsJsonObject()
                .getJsonArray("assignmentCandidates")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(assignmentCandidate -> new AssignmentCandidate(UUID.fromString(assignmentCandidate.getString("caseId")), assignmentCandidate.getInt("caseStreamVersion")))
                .collect(toList());
    }

}
