package uk.gov.moj.cpp.sjp.command.api.service;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionCourt;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

public class ReferenceDataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;

    public Optional<SessionCourt> getCourtByCourtHouseOUCode(final String courtHouseOUCode, final JsonEnvelope envelope) {
        final JsonObject queryParams = createObjectBuilder().add("oucode", courtHouseOUCode).build();
        final JsonEnvelope query = enveloper.withMetadataFrom(envelope, "referencedata.query.organisationunits").apply(queryParams);
        final JsonEnvelope organisationUnitsResponse = requester.requestAsAdmin(query);
        return organisationUnitsResponse.payloadAsJsonObject()
                .getJsonArray("organisationunits")
                .getValuesAs(JsonObject.class).stream()
                .findFirst()
                .map(ou -> new SessionCourt(ou.getString("oucodeL3Name"), ou.getString("lja")));
    }

}
