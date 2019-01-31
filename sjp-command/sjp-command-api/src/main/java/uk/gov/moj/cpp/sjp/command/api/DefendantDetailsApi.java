package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.moj.cpp.sjp.command.api.service.AddressService.normalizePostcodeInAddress;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class DefendantDetailsApi {

    private static final String ADDRESS = "address";

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.update-defendant-details")
    public void updateDefendantDetails(final JsonEnvelope envelope) {
        JsonObject payload = envelope.payloadAsJsonObject();

        if (payload.containsKey(ADDRESS)) {
            final JsonObjectBuilder updateDefendantObjectBuilder = createObjectBuilderWithFilter(payload, field -> !ADDRESS.equals(field));
            updateDefendantObjectBuilder.add(ADDRESS, normalizePostcodeInAddress(payload.getJsonObject(ADDRESS)));
            payload = updateDefendantObjectBuilder.build();
        }

        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.update-defendant-details").apply(payload));
    }

    @Handles("sjp.acknowledge-defendant-details-updates")
    public void acknowledgeDefendantDetailsUpdates(final JsonEnvelope envelope) {
        sender.send(
                enveloper.withMetadataFrom(
                        envelope,
                        "sjp.command.acknowledge-defendant-details-updates")
                        .apply(envelope.payloadAsJsonObject()));
    }

    @Handles("sjp.update-defendant-national-insurance-number")
    public void updateDefendantNationalInsuranceNumber(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.update-defendant-national-insurance-number").apply(envelope.payloadAsJsonObject()));
    }
}
