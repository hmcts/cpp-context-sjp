package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.api.validator.SetPleasModel;
import uk.gov.moj.cpp.sjp.command.api.validator.SetPleasValidator;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class SetPleasApi {


    @Inject
    private Enveloper enveloper;

    @Inject
    private SetPleasValidator setPleasValidator;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private Sender sender;

    @Handles("sjp.set-pleas")
    public void setPleas(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final SetPleasModel setPleasModel = new SetPleasModel(payload);

        final Map<String, List<String>> validationErrors = setPleasValidator.validate(setPleasModel);

        if (validationErrors.isEmpty()) {
            sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.set-pleas").apply(envelope.payloadAsJsonObject()));
        } else {
            throw new BadRequestException(objectToJsonValueConverter.convert(validationErrors).toString());
        }

    }

}
