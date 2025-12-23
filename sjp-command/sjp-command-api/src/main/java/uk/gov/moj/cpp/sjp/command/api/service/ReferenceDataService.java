package uk.gov.moj.cpp.sjp.command.api.service;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.SessionCourt;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReferenceDataService {

    @Inject
    private Enveloper enveloper;

    @Inject
    @ServiceComponent(COMMAND_API)
    private Requester requester;


    private static final Logger LOGGER = LoggerFactory.getLogger(ReferenceDataService.class.getName());
    private static final String REFERENCEDATA_QUERY_OFFENCE = "referencedataoffences.query.offences-list";

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

    public JsonObject getOffenceDetail(final JsonEnvelope envelope, final String offenceCode) {
        LOGGER.info(" Calling {} to get prosecutors for {} ", REFERENCEDATA_QUERY_OFFENCE, offenceCode);

        final JsonEnvelope request = envelopeFrom(metadataFrom(envelope.metadata())
                        .withName(REFERENCEDATA_QUERY_OFFENCE),
                createObjectBuilder().
                        add("cjsoffencecode", offenceCode));

        final Envelope<JsonObject> responseEnvelope = requester.requestAsAdmin(request, JsonObject.class);
        final JsonObject offenceData = responseEnvelope.payload();
        return offenceData.getJsonArray("offences").getValuesAs(JsonObject.class)
                .stream()
                .findFirst()
                .orElse(null);
    }
}
