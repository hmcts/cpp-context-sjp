package uk.gov.moj.cpp.sjp.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
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
public class EmployerApi {

    private static final String ADDRESS = "address";
    private static final String CASE_ID = "caseId";
    private static final String DEFENDANT_ID = "defendantId";

    private static final String SJP_COMMAND_UPDATE_EMPLOYER = "sjp.command.update-employer";
    private static final String SJP_COMMAND_DELETE_EMPLOYER = "sjp.command.delete-employer";

    @Inject
    private Enveloper enveloper;

    @Inject
    private Sender sender;

    @Handles("sjp.update-employer")
    public void updateEmployer(final JsonEnvelope envelope) {
        final JsonObject payloadAsJsonObject = envelope.payloadAsJsonObject();
        //TODO ATCM-3151: when the UI is adapted remove this code below
        final JsonObjectBuilder employerDetails = createObjectBuilderWithFilter(payloadAsJsonObject,
                key -> !(CASE_ID.equals(key) || DEFENDANT_ID.equals(key) || ADDRESS.equals(key)));

        if (payloadAsJsonObject.containsKey(ADDRESS)) {
            employerDetails.add(ADDRESS, normalizePostcodeInAddress(payloadAsJsonObject.getJsonObject(ADDRESS)));
        }

        final JsonObject payload = createObjectBuilder()
                .add(CASE_ID, payloadAsJsonObject.getString(CASE_ID))
                .add(DEFENDANT_ID, payloadAsJsonObject.getString(DEFENDANT_ID))
                .add("employer", employerDetails)
                .build();

        sender.send(enveloper.withMetadataFrom(envelope, SJP_COMMAND_UPDATE_EMPLOYER)
                .apply(payload));
    }

    @Handles("sjp.delete-employer")
    public void deleteEmployer(final JsonEnvelope envelope) {
        sender.send(enveloper.withMetadataFrom(envelope, SJP_COMMAND_DELETE_EMPLOYER).apply(envelope.payloadAsJsonObject()));
    }

}
