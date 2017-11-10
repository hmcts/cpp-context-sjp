package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;

import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaModel;
import uk.gov.moj.cpp.sjp.command.api.validator.UpdatePleaValidator;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

@ServiceComponent(COMMAND_API)
public class UpdatePleaApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private UpdatePleaValidator updatePleaValidator;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Inject
    private Sender sender;

    @Handles("sjp.update-plea")
    public void updatePlea(final JsonEnvelope envelope) {
        final UpdatePleaModel updatePleaModel = new UpdatePleaModel(envelope.payloadAsJsonObject());

        final Map<String, List<String>> validationErrors = updatePleaValidator.validate(updatePleaModel);

        if (validationErrors.isEmpty()) {
            sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.update-plea").apply(envelope.payloadAsJsonObject()));
        } else {
            throw new BadRequestException(objectToJsonValueConverter.convert(validationErrors).toString());
        }
    }

    @Handles("sjp.cancel-plea")
    public void cancelPlea(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "sjp.command.cancel-plea").apply(envelope.payloadAsJsonObject()));
    }

}
