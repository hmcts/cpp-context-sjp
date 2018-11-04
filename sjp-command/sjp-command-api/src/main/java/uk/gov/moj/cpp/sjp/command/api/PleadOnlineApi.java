package uk.gov.moj.cpp.sjp.command.api;

import static java.util.Arrays.asList;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.moj.cpp.sjp.command.api.service.AddressService.normalizePostcodeInAddress;

import uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.command.api.validator.PleadOnlineValidator;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class PleadOnlineApi {

    private static final String ADDRESS = "address";
    private static final String PERSONAL_DETAILS = "personalDetails";
    private static final String EMPLOYER = "employer";

    @Inject
    private Sender sender;

    @Inject
    private PleadOnlineValidator pleadOnlineValidator;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private ObjectToJsonObjectConverter objectToJsonObjectConverter;

    @Handles("sjp.plead-online")
    public void pleadOnline(final Envelope<PleadOnline> envelope) {
        final PleadOnline pleadOnline = envelope.payload();
        final JsonObject payload = objectToJsonObjectConverter.convert(envelope.payload());

        final Map<String, List<String>> validationErrors = pleadOnlineValidator.validate(pleadOnline);

        if (!validationErrors.isEmpty()) {
            throw new BadRequestException(objectToJsonValueConverter.convert(validationErrors).toString());
        }

        final JsonObjectBuilder pleaOnlineObjectBuilder = createObjectBuilderWithFilter(payload, field -> !asList(PERSONAL_DETAILS, EMPLOYER).contains(field));

        pleaOnlineObjectBuilder.add(PERSONAL_DETAILS, replacePostcodeInPayload(payload, PERSONAL_DETAILS));
        if (payload.containsKey(EMPLOYER)) {
            pleaOnlineObjectBuilder.add(EMPLOYER, replacePostcodeInPayload(payload, EMPLOYER));
        }

        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("sjp.command.plead-online").build(),
                pleaOnlineObjectBuilder.build()));
    }


    private JsonObjectBuilder replacePostcodeInPayload(final JsonObject payload, final String objectToUpdate) {
        final JsonObjectBuilder objectToUpdateBuilder = createObjectBuilderWithFilter(payload.getJsonObject(objectToUpdate),
                field -> !field.contains(ADDRESS));

        objectToUpdateBuilder.add(ADDRESS, normalizePostcodeInAddress(payload.getJsonObject(objectToUpdate).getJsonObject(ADDRESS)));

        return objectToUpdateBuilder;
    }

}
