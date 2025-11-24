package uk.gov.moj.cpp.sjp.query.controller;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_CONTROLLER)
public class CaseAssignmentRestrictionController {

    @Inject
    private Requester requester;

    @Handles("sjp.query.case-assignment-restriction")
    public JsonEnvelope getCaseAssignmentRestriction(final JsonEnvelope query) {
        return requester.request(query);
    }
}
