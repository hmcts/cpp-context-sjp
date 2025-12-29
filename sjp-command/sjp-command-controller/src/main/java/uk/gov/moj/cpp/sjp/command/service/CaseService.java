package uk.gov.moj.cpp.sjp.command.service;

import static java.util.UUID.randomUUID;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;
import static uk.gov.justice.services.core.annotation.Component.COMMAND_CONTROLLER;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataBuilder;

import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;
import javax.json.JsonObject;

public class CaseService {

    @Inject
    @ServiceComponent(COMMAND_CONTROLLER)
    private Requester requester;

    public JsonObject getCaseDetails(final String caseId){
        final JsonObject queryParams = createObjectBuilder().add("caseId", caseId).build();
        final JsonEnvelope query = envelopeFrom(metadataBuilder().withId(randomUUID()).withName("sjp.query.case"), queryParams);

        return requester.request(query).payloadAsJsonObject();
    }

}
