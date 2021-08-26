package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonValue;

public class ProsecutionCaseFileService {

    @Inject
    @ServiceComponent(Component.QUERY_VIEW)
    private Requester requester;

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    public Optional<JsonObject> getCaseFileDetails(final UUID caseId) {
        final JsonObject payload = createObjectBuilder().add("caseId", caseId.toString()).build();
        final JsonEnvelope request = envelopeFrom(
                metadataBuilder()
                        .withId(randomUUID())
                        .withName("prosecutioncasefile.query.case"), payload);

        final JsonEnvelope response = requester.requestAsAdmin(request);

        return response.payload() != JsonValue.NULL
                ? ofNullable(response.payloadAsJsonObject())
                : empty();
    }

}
