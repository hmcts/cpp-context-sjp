package uk.gov.moj.cpp.sjp.command.api;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.messaging.JsonObjects.createArrayBuilder;
import static java.util.Objects.nonNull;

import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilderWithFilter;
import static uk.gov.justice.services.messaging.JsonObjects.getLong;
import static uk.gov.moj.cpp.sjp.command.api.service.AddressService.normalizePostcodeInAddress;
import static uk.gov.moj.cpp.sjp.command.api.service.ContactDetailsService.convertBlankEmailsToNull;
import static uk.gov.justice.services.messaging.JsonObjects.getBoolean;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.command.api.service.ReferenceDataService;

import java.util.Optional;

import javax.inject.Inject;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

@ServiceComponent(COMMAND_API)
public class CreateCaseApi {

    private static final String DEFENDANT = "defendant";
    private static final String ADDRESS = "address";
    private static final String CONTACT_DETAILS = "contactDetails";
    private static final String OFFENCES = "offences";
    private static final String ISELIGIBLEAOCP = "isEligibleAOCP";
    private static final String AOCP_STANDARDPENALTY_AMOUNT = "aocpStandardPenaltyAmount";

    @Inject
    private Sender sender;

    @Inject
    private ReferenceDataService referenceDataService;

    @Handles("sjp.create-sjp-case")
    public void createSjpCase(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final JsonObject defendantObject = payload.getJsonObject(DEFENDANT);

        final JsonObjectBuilder defendantObjectBuilder = createObjectBuilderWithFilter(defendantObject, field -> !ADDRESS.equals(field));
        defendantObjectBuilder.add(ADDRESS, normalizePostcodeInAddress(defendantObject.getJsonObject(ADDRESS)));
        ofNullable(defendantObject.getJsonObject(CONTACT_DETAILS))
                .ifPresent(contactDetails -> defendantObjectBuilder.add(CONTACT_DETAILS, convertBlankEmailsToNull(contactDetails)));

        final JsonObjectBuilder createCaseObjectBuilder = createObjectBuilderWithFilter(payload, field -> !DEFENDANT.equals(field));

        final JsonArray offenceArrayWithAOCPDetails = populateOffenceWithAOCPDetails(envelope, defendantObject.getJsonArray(OFFENCES));
        if (!offenceArrayWithAOCPDetails.isEmpty()) {
            final JsonObjectBuilder offenceObjectBuilder = createObjectBuilderWithFilter(defendantObjectBuilder.build(), field -> !OFFENCES.equals(field));
            offenceObjectBuilder.add(OFFENCES, offenceArrayWithAOCPDetails);
            createCaseObjectBuilder.add(DEFENDANT, offenceObjectBuilder);
        } else {
            createCaseObjectBuilder.add(DEFENDANT, defendantObjectBuilder);
        }

        sender.send(JsonEnvelope.envelopeFrom(metadataFrom(envelope.metadata())
                .withName("sjp.command.create-sjp-case"), createCaseObjectBuilder.build()
        ));

    }

    private JsonArray populateOffenceWithAOCPDetails(final JsonEnvelope envelope, final JsonArray offences) {
        final JsonArrayBuilder arrayBuilder = createArrayBuilder();
        if (nonNull(offences)) {
            offences.getValuesAs(JsonObject.class).forEach(offence ->
                    arrayBuilder.add(getOffenceDetails(envelope, offence)));
        }
        return arrayBuilder.build();
    }

    @SuppressWarnings("squid:S1126")
    private JsonObject getOffenceDetails(final JsonEnvelope envelope, final JsonObject offence) {
        final JsonObjectBuilder offenceBuilder = createObjectBuilderWithFilter(offence, field -> true);
        final JsonObject referenceDataOffence = referenceDataService.getOffenceDetail(envelope, offence.getString("libraOffenceCode"));
        if (nonNull(referenceDataOffence)) {
            final Optional<Boolean> isOffenceEligibleForAOCP = getBoolean(referenceDataOffence, "aocpEligible");
            final Optional<Long> standardPenalty = getLong(referenceDataOffence, "aocpStandardPenalty");
            if (isOffenceEligibleForAOCP.isPresent()) {
                if (Boolean.TRUE.equals(isOffenceEligibleForAOCP.get())) {
                    offenceBuilder.add(ISELIGIBLEAOCP, true);
                } else {
                    offenceBuilder.add(ISELIGIBLEAOCP, false);
                }
            }
            if (standardPenalty.isPresent()) {
                offenceBuilder.add(AOCP_STANDARDPENALTY_AMOUNT, standardPenalty.get());
            }
        }
        return offenceBuilder.build();
    }
}
