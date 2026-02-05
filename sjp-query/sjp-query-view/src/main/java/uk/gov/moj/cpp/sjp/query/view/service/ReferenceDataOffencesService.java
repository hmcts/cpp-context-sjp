package uk.gov.moj.cpp.sjp.query.view.service;

import static java.util.UUID.randomUUID;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataOffencesService {

    @Inject
    @ServiceComponent(Component.QUERY_VIEW)
    private Requester requester;

    public Map<String, JsonObject> getOffenceDefinitionsByOffenceCode(final Set<String> offenceCodes, final LocalDate date) {
        return offenceCodes.stream().collect(toMap(identity(), offenceCode -> getOffenceReferenceData(offenceCode, date.toString())));
    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    public JsonObject getOffenceReferenceData(final String offenceCode, final String date) {
        final JsonEnvelope request = JsonEnvelope.envelopeFrom(metadataBuilder()
                .withId(randomUUID())
                .withName("referencedataoffences.query.offences-list"),
                createObjectBuilder()
                        .add("cjsoffencecode", offenceCode)
                        .add("date", date)
                        .build()
        );
        final JsonEnvelope response = requester.requestAsAdmin(request);
        return response.payloadAsJsonObject().getJsonArray("offences").getValuesAs(JsonObject.class)
                .stream()
                .findFirst()
                .orElse(null);
    }
}
