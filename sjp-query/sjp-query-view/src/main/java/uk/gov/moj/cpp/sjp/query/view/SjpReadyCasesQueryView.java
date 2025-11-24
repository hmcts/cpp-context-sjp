package uk.gov.moj.cpp.sjp.query.view;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.toJsonArray;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.repository.ReadyCaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@SuppressWarnings("WeakerAccess")
@ServiceComponent(Component.QUERY_VIEW)
public class SjpReadyCasesQueryView {

    @Inject
    private ReadyCaseRepository readyCaseRepository;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.query.ready-cases")
    public JsonEnvelope getReadyCases(final JsonEnvelope envelope) {

        final JsonObject queryOptions = envelope.payloadAsJsonObject();

        final Optional<UUID> assigneeId = Optional.ofNullable(queryOptions.getString("assigneeId", null)).map(UUID::fromString);

        final List<ReadyCase> readyCases = assigneeId.isPresent() ? readyCaseRepository.findByAssigneeId(assigneeId.get()) : readyCaseRepository.findAll();

        final JsonArray readyCasesArray = toJsonArray(readyCases,
                readyCase -> {
                    final JsonObjectBuilder builder = createObjectBuilder();
                    builder.add("caseId", readyCase.getCaseId().toString())
                            .add("reason", readyCase.getReason().name())
                            .add("sessionType", readyCase.getSessionType().name())
                            .add("prosecutingAuthority", readyCase.getProsecutionAuthority());

                    readyCase.getAssigneeId().ifPresent(assignee -> builder.add("assigneeId", assignee.toString()));
                    return builder.build();
                });

        final JsonObject readyCasesPayload = createObjectBuilder().add("readyCases", readyCasesArray).build();

        return enveloper.withMetadataFrom(envelope, "sjp.query.ready-cases")
                .apply(readyCasesPayload);
    }

}
