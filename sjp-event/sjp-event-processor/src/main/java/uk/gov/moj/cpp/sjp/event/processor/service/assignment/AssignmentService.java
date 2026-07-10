package uk.gov.moj.cpp.sjp.event.processor.service.assignment;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.moj.cpp.sjp.event.processor.EventProcessorConstants.CASE_ID;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.Metadata;
import uk.gov.moj.cpp.sjp.domain.AssignmentCandidate;
import uk.gov.moj.cpp.sjp.domain.SessionType;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;

public class AssignmentService {

    @Inject
    private AssignmentConfiguration assignmentConfiguration;

    @Inject
    private Enveloper enveloper;

    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Requester requester;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Sender sender;

    public List<AssignmentCandidate> getAssignmentCandidates(final JsonEnvelope envelope, final UUID legalAdviserId, final SessionType sessionType, final String localJusticeAreaNationalCourtCode, final JsonArray prosecutors) {

        final int assignmentCandidatesLimit = assignmentConfiguration.getAssignmentCandidatesLimit();

        final JsonObjectBuilder queryOptions = createObjectBuilder()
                .add("sessionType", sessionType.name())
                .add("assigneeId", legalAdviserId.toString())
                .add("localJusticeAreaNationalCourtCode", localJusticeAreaNationalCourtCode)
                .add("limit", assignmentCandidatesLimit);

        ofNullable(prosecutors).ifPresent(p -> queryOptions.add("prosecutors", p.stream()
                .map(JsonString.class::cast)
                .map(JsonString::getString)
                .collect(joining(","))));

        return requester.request(enveloper.withMetadataFrom(envelope, "sjp.query.assignment-candidates")
                .apply(queryOptions.build()))
                .payloadAsJsonObject()
                .getJsonArray("assignmentCandidates")
                .getValuesAs(JsonObject.class)
                .stream()
                .map(assignmentCandidate -> new AssignmentCandidate(UUID.fromString(assignmentCandidate.getString("caseId")), assignmentCandidate.getInt("caseStreamVersion")))
                .collect(toList());
    }

    public void unassignCase(final UUID caseId) {

        final Metadata metadata = metadataBuilder().withId(randomUUID()).withName("sjp.command.unassign-case").build();

        final JsonObject payload = createObjectBuilder().add(CASE_ID, caseId.toString()).build();

        sender.sendAsAdmin(envelopeFrom(metadata, payload));
    }
}
