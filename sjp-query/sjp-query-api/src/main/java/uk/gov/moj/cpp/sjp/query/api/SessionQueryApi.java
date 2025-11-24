package uk.gov.moj.cpp.sjp.query.api;

import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.requester.Requester;
import uk.gov.justice.services.messaging.JsonEnvelope;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_API)
public class SessionQueryApi {

    @Inject
    private Requester requester;

    @Handles("sjp.query.session")
    public JsonEnvelope findSession(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.latest-aocp-session")
    public JsonEnvelope findLatestAocpSession(final JsonEnvelope query) {
        return requester.request(query);
    }

    @Handles("sjp.query.convicting-court-session")
    public JsonEnvelope findConvictingCourtSession(final JsonEnvelope query) {
        return requester.request(query);
    }
}
