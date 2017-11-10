package uk.gov.moj.cpp.sjp.event.processor;

import static javax.json.Json.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.messaging.JsonObjects;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(EVENT_PROCESSOR)
public class PersonalDetailsUpdatedProcessor {

    @Inject
    private Sender sender;

    @Inject
    private Requester requester;

    @Inject
    private Enveloper enveloper;

    @Handles("people.personal-details-updated")
    public void personalDetailsUpdated(JsonEnvelope envelope) {
        final JsonObject personalDetailsUpdatedPayload = envelope.payloadAsJsonObject();
        final String defendantId = personalDetailsUpdatedPayload.getString("personId");

        final JsonObjectBuilder updatePersonInfoRequestPayloadBuilder = createObjectBuilder()
                .add("personId", defendantId);

        JsonObjects.getString(personalDetailsUpdatedPayload, "firstName").ifPresent(firstName ->
                updatePersonInfoRequestPayloadBuilder.add("firstName", firstName)
        );
        JsonObjects.getString(personalDetailsUpdatedPayload, "lastName").ifPresent(lastName ->
                updatePersonInfoRequestPayloadBuilder.add("lastName", lastName)
        );

        JsonObjects.getString(personalDetailsUpdatedPayload, "dateOfBirth").ifPresent(dateOfBirth ->
                updatePersonInfoRequestPayloadBuilder.add("dateOfBirth", dateOfBirth)
        );

        JsonObjects.getString(personalDetailsUpdatedPayload, "address", "postCode").ifPresent(postCode ->
                updatePersonInfoRequestPayloadBuilder.add("postCode", postCode)
        );

        final JsonEnvelope updatePersonInfoRequest = enveloper.withMetadataFrom(envelope, "sjp.command.update-person-info")
                .apply(updatePersonInfoRequestPayloadBuilder.build());

        sender.sendAsAdmin(updatePersonInfoRequest);
    }
}
