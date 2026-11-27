package uk.gov.moj.cpp.sjp.event.processor.service;

import static java.util.UUID.fromString;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataOffencesService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(Component.EVENT_PROCESSOR)
    private Requester requester;

    public Map<String, JsonObject> getOffenceDefinitionByOffenceCode(final Set<String> offenceCodes, final LocalDate referredAt, final JsonEnvelope envelope) {
        return offenceCodes.stream().collect(toMap(identity(), offenceCode -> getOffenceReferenceData(envelope, offenceCode, referredAt.toString()).get()));
    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    public Optional<JsonObject> getOffenceReferenceData(final JsonEnvelope envelope, final String offenceCode, final String date) {
        final JsonEnvelope request = enveloper
                .withMetadataFrom(envelope, "referencedataoffences.query.offences-list")
                .apply(createObjectBuilder()
                        .add("cjsoffencecode", offenceCode)
                        .add("date", date)
                        .build());
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject().getJsonArray("offences").getValuesAs(JsonObject.class).stream().findFirst();
    }

    public UUID getOffenceDefinitionId(final JsonObject offenceDefinitionJsonObject) {
        return fromString(offenceDefinitionJsonObject.getString("offenceId"));
    }

    public String getMaxPenalty(final JsonObject offenceDefinitionJsonObject) {
        return offenceDefinitionJsonObject.getString("maxPenalty", null);
    }
}
