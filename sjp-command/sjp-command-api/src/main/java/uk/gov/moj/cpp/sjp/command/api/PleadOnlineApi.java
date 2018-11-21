package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;

import uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.moj.cpp.sjp.command.api.validator.PleadOnlineValidator;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

@ServiceComponent(COMMAND_API)
public class PleadOnlineApi {

    @Inject
    private Sender sender;

    @Inject
    private PleadOnlineValidator pleadOnlineValidator;

    @Inject
    private ObjectToJsonValueConverter objectToJsonValueConverter;

    @Handles("sjp.plead-online")
    public void pleadOnline(final Envelope<PleadOnline> envelope) {
        final PleadOnline pleadOnline = envelope.payload();

        final Map<String, List<String>> validationErrors = pleadOnlineValidator.validate(pleadOnline);

        if (!validationErrors.isEmpty()) {
            throw new BadRequestException(objectToJsonValueConverter.convert(validationErrors).toString());
        }

        sender.send(envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("sjp.command.plead-online").build(),
                pleadOnline));
    }

}
