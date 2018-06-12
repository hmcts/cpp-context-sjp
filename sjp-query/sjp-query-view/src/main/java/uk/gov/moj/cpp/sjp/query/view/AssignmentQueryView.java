package uk.gov.moj.cpp.sjp.query.view;


import static java.util.stream.Collectors.toSet;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.query.view.service.AssignmentService;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;

@ServiceComponent(Component.QUERY_VIEW)
public class AssignmentQueryView {

    @Inject
    private Enveloper enveloper;

    @Inject
    private AssignmentService assignmentService;

    @Inject
    private CaseService caseService;

    @Handles("sjp.query.case-assignment")
    public JsonEnvelope getCaseAssignment(final JsonEnvelope query) {
        final UUID caseId = UUID.fromString(query.payloadAsJsonObject().getString("caseId"));
        final UUID userId = query.metadata().userId().map(UUID::fromString).orElse(null);

        final Optional<CaseDetail> caseDetail = caseService.getCase(caseId);

        final JsonObject caseAssignment = caseDetail
                .map(cd -> Optional.ofNullable(cd.getAssigneeId()))
                .map(assigneeId -> createObjectBuilder()
                        .add("assigned", assigneeId.isPresent())
                        .add("assignedToMe", assigneeId.filter(assignee -> assignee.equals(userId)).isPresent())
                        .build())
                .orElse(null);

        return enveloper.withMetadataFrom(query, "sjp.query.case-assignment").apply(caseAssignment);
    }

    @Handles("sjp.query.assignment-candidates")
    public JsonEnvelope findAssignmentCandidates(final JsonEnvelope envelope) {
        final JsonObject queryOptions = envelope.payloadAsJsonObject();

        final UUID assigneeId = UUID.fromString(queryOptions.getString("assigneeId"));
        final SessionType sessionType = SessionType.valueOf(queryOptions.getString("sessionType"));
        final int limit = queryOptions.getInt("limit");
        final String excludedProsecutingAuthoritiesAsString = queryOptions.getString("excludedProsecutingAuthorities", "");

        final Set<String> excludedProsecutingAuthorities = Stream.of(excludedProsecutingAuthoritiesAsString.split(","))
                .map(String::trim)
                .filter(prosecutor -> !prosecutor.isEmpty())
                .collect(toSet());

        final List<AssignmentCandidate> assignmentCandidatesList = assignmentService.getAssignmentCandidates(assigneeId, sessionType, excludedProsecutingAuthorities, limit);

        final JsonArrayBuilder casesReadyForDecisionBuilder = Json.createArrayBuilder();

        assignmentCandidatesList.forEach(assignmentCandidate -> casesReadyForDecisionBuilder.add(createObjectBuilder()
                .add("caseId", assignmentCandidate.getCaseId().toString())
                .add("caseStreamVersion", assignmentCandidate.getCaseStreamVersion())
        ));

        final JsonObject casesReadyForDecision = createObjectBuilder()
                .add("assignmentCandidates", casesReadyForDecisionBuilder.build())
                .build();

        return enveloper.withMetadataFrom(envelope, "sjp.query.assignment-candidates").apply(casesReadyForDecision);
    }
}
