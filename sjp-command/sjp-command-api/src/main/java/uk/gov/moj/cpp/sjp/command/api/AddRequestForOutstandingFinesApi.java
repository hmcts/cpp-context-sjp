package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import java.time.LocalDate;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(COMMAND_API)
public class AddRequestForOutstandingFinesApi {

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.add-request-for-outstanding-fines")
    public void addRequestForOutstandingFines(final JsonEnvelope envelope) {
        JsonObject commandObject = envelope.payloadAsJsonObject();
        if (commandObject.get("hearingDate") == null || commandObject.isNull("hearingDate")) {
            commandObject = createObjectBuilder(commandObject)
                    .add("hearingDate", LocalDate.now().plusDays(1).toString()) // default next Day
                    .build();
        }
        this.sender.send(this.enveloper.withMetadataFrom(envelope, "sjp.command.add-request-for-outstanding-fines").apply(commandObject));
    }

}
