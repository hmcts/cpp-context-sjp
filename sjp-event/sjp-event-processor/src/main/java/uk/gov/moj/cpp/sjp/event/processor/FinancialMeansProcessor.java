package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonObject;

@ServiceComponent(EVENT_PROCESSOR)
public class FinancialMeansProcessor {

    private static final String MATERIAL_IDS = "materialIds";
    private static final String MATERIAL_ID = "materialId";

    @Inject
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    @Handles("sjp.events.financial-means-updated")
    public void updateFinancialMeans(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, "public.sjp.financial-means-updated")
                .apply(envelope.payloadAsJsonObject()));
    }

    @Handles("sjp.events.all-financial-means-updated")
    public void updateAllFinancialMeans(final JsonEnvelope event) {
        final JsonObject newPayload = createObjectBuilder()
                .add("defendantId", event.payloadAsJsonObject().getString("defendantId"))
                .build();
        final JsonEnvelope newEventEnvelope = enveloper.withMetadataFrom(event,
                "public.sjp.all-financial-means-updated").apply(newPayload);
        sender.send(newEventEnvelope);
    }

    @Handles("sjp.events.financial-means-deleted")
    public void deleteFinancialMeans(final JsonEnvelope envelope) {

        //Check if any materials have to be deleted
        if (envelope.payloadAsJsonObject().getJsonArray(MATERIAL_IDS) != null &&
                !envelope.payloadAsJsonObject().getJsonArray(MATERIAL_IDS).isEmpty()) {
            final JsonArray materialIds = envelope.payloadAsJsonObject().getJsonArray(MATERIAL_IDS);
            for (int i = 0; i < materialIds.size(); i++) {

                final String materialId = materialIds.getString(i);
                final JsonObject requestPayload = createObjectBuilder()
                        .add(MATERIAL_ID, materialId).build();

                //Sending the Payload to the Materials Context
                sender.send(
                        Enveloper.envelop(requestPayload)
                                .withName("material.command.delete-material")
                                .withMetadataFrom(envelope));
            }
        }
        //Publish a public event to announce that the Financial Means has been deleted.....
        sender.send(Enveloper.envelop(envelope.payloadAsJsonObject())
                .withName("public.sjp.events.defendant-financial-means-deleted")
                .withMetadataFrom(envelope));

    }

}
