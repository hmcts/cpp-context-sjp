package uk.gov.moj.cpp.sjp.query.controller;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_CONTROLLER)
public class SjpDocumentController {

    @Inject
    private Requester requester;

    @Handles("sjp.query.case-document")
    public JsonEnvelope findCaseDocument(final JsonEnvelope query) {
        return requester.request(query);
    }
}
