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
public class CreateCaseApi {

    private static final String DEFENDANT = "defendant";
    private static final String ADDRESS = "address";

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.create-sjp-case")
    public void createSjpCase(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonObject defendantObject = payload.getJsonObject(DEFENDANT);

        final JsonObjectBuilder defendantObjectBuilder = createObjectBuilderWithFilter(defendantObject, field -> !ADDRESS.equals(field));
        defendantObjectBuilder.add(ADDRESS, normalizePostcodeInAddress(defendantObject.getJsonObject(ADDRESS)));

        final JsonObjectBuilder createCaseObjectBuilder = createObjectBuilderWithFilter(payload, field -> !DEFENDANT.equals(field));
        createCaseObjectBuilder.add(DEFENDANT, defendantObjectBuilder);

        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.create-sjp-case").apply(createCaseObjectBuilder.build()));
    }

}
