package uk.gov.moj.cpp.sjp.query.view;


import static java.util.Arrays.stream;
import static javax.json.Json.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.SessionType;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.ReserveCaseRepository;
import uk.gov.moj.cpp.sjp.query.view.service.AssignmentService;
import uk.gov.moj.cpp.sjp.query.view.service.CaseService;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    @Inject
    private ReserveCaseRepository reserveCaseRepository;

    @Handles("sjp.query.case-assignment")
    public JsonEnvelope getCaseAssignment(final JsonEnvelope query) {
        final UUID caseId = UUID.fromString(query.payloadAsJsonObject().getString("caseId"));
        final UUID userId = query.metadata().userId().map(UUID::fromString).orElse(null);

        final Optional<CaseDetail> caseDetail = caseService.getCase(caseId);
        final int reservedCases = getReservedCases(userId);

        final JsonObject caseAssignment = caseDetail
                .map(cd -> Optional.ofNullable(cd.getAssigneeId()))
                .map(assigneeId -> createObjectBuilder()
                        .add("assigned", assigneeId.isPresent())
                        .add("assignedToMe", assigneeId.filter(assignee -> assignee.equals(userId)).isPresent())
                        .add("reservedCases", reservedCases)
                        .build())
                .orElse(null);

        return enveloper.withMetadataFrom(query, "sjp.query.case-assignment").apply(caseAssignment);
    }

    @Handles("sjp.query.assignment-candidates")
    public JsonEnvelope findAssignmentCandidates(final JsonEnvelope envelope) {
        final JsonObject queryOptions = envelope.payloadAsJsonObject();

        final UUID assigneeId = UUID.fromString(queryOptions.getString("assigneeId"));
        final SessionType sessionType = SessionType.valueOf(queryOptions.getString("sessionType"));
        final String localJusticeAreaNationalCourtCode = queryOptions.getString("localJusticeAreaNationalCourtCode");
        final int limit = queryOptions.getInt("limit");
        final String prosecutors = queryOptions.getString("prosecutors", null);
        final Set<String> prosecutingAuthorities;

        if (Objects.isNull(prosecutors)) {
            prosecutingAuthorities = new HashSet<>(assignmentService.getProsecutingAuthorityByLja(localJusticeAreaNationalCourtCode));
        } else {
            prosecutingAuthorities = stream(prosecutors.split(",")).collect(Collectors.toSet());
        }

        final List<AssignmentCandidate> assignmentCandidatesList = assignmentService.getAssignmentCandidates(assigneeId, sessionType, prosecutingAuthorities, limit);


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

    private int getReservedCases(final UUID userId) {
        return reserveCaseRepository.countReservedBy(userId);
    }
}
