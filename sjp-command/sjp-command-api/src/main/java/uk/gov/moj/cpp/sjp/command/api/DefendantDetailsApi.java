package uk.gov.moj.cpp.sjp.command.api;

import static org.apache.commons.lang3.StringUtils.isWhitespace;
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

@SuppressWarnings("squid:CallToDeprecatedMethod")
@ServiceComponent(COMMAND_API)
public class DefendantDetailsApi {

    private static final String ADDRESS = "address";
    private static final String EMAIL = "email";

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

        if (payload.containsKey(EMAIL) && isWhitespace(payload.getString(EMAIL))) {
            final JsonObjectBuilder updateDefendantObjectBuilder = createObjectBuilderWithFilter(payload, field -> !EMAIL.equals(field));
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

    @Handles("sjp.accept-pending-defendant-changes")
    public void acceptPendingDefendantChanges(final JsonEnvelope envelope) {
        JsonObject payload = envelope.payloadAsJsonObject();

        if (payload.containsKey(ADDRESS)) {
            final JsonObjectBuilder updateDefendantObjectBuilder = createObjectBuilderWithFilter(payload, field -> !ADDRESS.equals(field));
            updateDefendantObjectBuilder.add(ADDRESS, normalizePostcodeInAddress(payload.getJsonObject(ADDRESS)));
            payload = updateDefendantObjectBuilder.build();
        }

        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.accept-pending-defendant-changes").apply(payload));
    }

    @Handles("sjp.reject-pending-defendant-changes")
    public void rejectPendingDefendantChanges(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.reject-pending-defendant-changes").apply(envelope.payloadAsJsonObject()));
    }

}
