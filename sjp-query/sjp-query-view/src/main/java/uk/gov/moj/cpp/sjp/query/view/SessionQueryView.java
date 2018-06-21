package uk.gov.moj.cpp.sjp.query.view;


import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.repository.SessionRepository;

import java.util.UUID;

import javax.inject.Inject;

@ServiceComponent(Component.QUERY_VIEW)
public class SessionQueryView {

    @Inject
    private Enveloper enveloper;

    @Inject
    private SessionRepository sessionRepository;

    @Handles("sjp.query.session")
    public JsonEnvelope findSession(final JsonEnvelope query) {
        final UUID sessionId = UUID.fromString(query.payloadAsJsonObject().getString("sessionId"));
        final Session session = sessionRepository.findBy(sessionId);
        return enveloper.withMetadataFrom(query, "sjp.query.session").apply(session);
    }
}
