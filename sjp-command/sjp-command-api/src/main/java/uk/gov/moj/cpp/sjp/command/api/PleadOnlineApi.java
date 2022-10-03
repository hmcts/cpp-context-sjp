package uk.gov.moj.cpp.sjp.command.api;

import static java.util.Arrays.asList;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.Envelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.moj.cpp.sjp.command.api.service.AddressService.normalizePostcodeInAddress;

import uk.gov.justice.json.schemas.domains.sjp.command.PleadOnline;
import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.converter.ObjectToJsonObjectConverter;
import uk.gov.justice.services.common.converter.ObjectToJsonValueConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.api.validator.PleadOnlineValidator;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class PleadOnlineApi {

    private static final String ADDRESS = "address";
    private static final String PERSONAL_DETAILS = "personalDetails";
    private static final String EMPLOYER = "employer";

    private static final String LEGAL_ENTITY = "legalEntityDefendant";

    @Inject
    private Sender sender;

    @Inject
    private Requester requester;

    @Inject
    private Enveloper enveloper;

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

        Map<String, List<String>> validationErrors;
        validationErrors = pleadOnlineValidator.validate(pleadOnline);

        checkValidationErrors(validationErrors);

        final JsonObject caseDetail = getCaseDetail(envelope);

        validationErrors = pleadOnlineValidator.validate(caseDetail);

        checkValidationErrors(validationErrors);

        final JsonObjectBuilder pleaOnlineObjectBuilder = createObjectBuilderWithFilter(payload, field -> !asList(PERSONAL_DETAILS, EMPLOYER).contains(field));
        if (payload.containsKey(PERSONAL_DETAILS))  {
            pleaOnlineObjectBuilder.add(PERSONAL_DETAILS, replacePostcodeInPayload(payload, PERSONAL_DETAILS));
        }

        if (payload.containsKey(EMPLOYER)) {
            pleaOnlineObjectBuilder.add(EMPLOYER, replacePostcodeInPayload(payload, EMPLOYER));
        }

        if (payload.containsKey(LEGAL_ENTITY)) {
            pleaOnlineObjectBuilder.add(LEGAL_ENTITY, replacePostcodeInPayload(payload, LEGAL_ENTITY));
        }

        sender.send(Envelope.envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("sjp.command.plead-online").build(),
                pleaOnlineObjectBuilder.build()));
    }

    private void checkValidationErrors(Map<String, List<String>> validationErrors) {
        if (!validationErrors.isEmpty()) {
            throw new BadRequestException(objectToJsonValueConverter.convert(validationErrors).toString());
        }
    }


    private JsonObject getCaseDetail(final Envelope<PleadOnline> envelope) {
        final JsonObject queryCasePayload = Json.createObjectBuilder()
                .add("caseId", envelope.payload().getCaseId().toString())
                .build();

        final JsonEnvelope queryCaseEnvelope = JsonEnvelope.envelopeFrom(
                metadataFrom(envelope.metadata())
                        .withName("sjp.query.case").build(),
                queryCasePayload);

        return requester.requestAsAdmin(queryCaseEnvelope).payloadAsJsonObject();
    }


    private JsonObjectBuilder replacePostcodeInPayload(final JsonObject payload, final String objectToUpdate) {
        final JsonObjectBuilder objectToUpdateBuilder = createObjectBuilderWithFilter(payload.getJsonObject(objectToUpdate),
                field -> !field.contains(ADDRESS));

        objectToUpdateBuilder.add(ADDRESS, normalizePostcodeInAddress(payload.getJsonObject(objectToUpdate).getJsonObject(ADDRESS)));

        return objectToUpdateBuilder;
    }

}
