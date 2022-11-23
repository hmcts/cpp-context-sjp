package uk.gov.moj.cpp.sjp.query.view;


import uk.gov.justice.services.core.annotation.Component;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDecision;
import uk.gov.moj.cpp.sjp.persistence.entity.Session;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseDecisionRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.SessionRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(Component.QUERY_VIEW)
public class SessionQueryView {

    @Inject
    private Enveloper enveloper;

    @Inject
    private SessionRepository sessionRepository;

    @Inject
    private CaseDecisionRepository caseDecisionRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionQueryView.class);

    @Handles("sjp.query.session")
    public JsonEnvelope findSession(final JsonEnvelope query) {
        final UUID sessionId = UUID.fromString(query.payloadAsJsonObject().getString("sessionId"));
        final Session session = sessionRepository.findBy(sessionId);
        return enveloper.withMetadataFrom(query, "sjp.query.session").apply(session);
    }

    @SuppressWarnings("squid:CallToDeprecatedMethod")
    @Handles("sjp.query.latest-aocp-session")
    public JsonEnvelope getLatestAocpSession(final JsonEnvelope query) {
        Session session = null;
        final List<Session> sessionList = sessionRepository.findLatestAocpSession();
        if (sessionList.isEmpty()) {
            LOGGER.info("AOCP session record not found");
        } else {
            session = sessionList.get(0);
        }
            return enveloper.withMetadataFrom(query, "sjp.query.latest-aocp-session").apply(session);
    }

    @Handles("sjp.query.convicting-court-session")
    public JsonEnvelope findConvictingCourtSession(final JsonEnvelope query) {
        final UUID offenceId = UUID.fromString(query.payloadAsJsonObject().getString("offenceId"));
        final List<CaseDecision> caseDecisions = caseDecisionRepository.findCaseDecisionsForConvictingCourtSessions(offenceId);

        final Optional<CaseDecision> lastCaseDecisionOptional = caseDecisions.stream()
                .max(Comparator.comparing(CaseDecision::getSavedAt));


        Session convictingCourtSession = null;
        if (lastCaseDecisionOptional.isPresent()) {
            convictingCourtSession = lastCaseDecisionOptional.get().getSession();
        }

        return enveloper.withMetadataFrom(query, "sjp.query.convicting-court-session").apply(convictingCourtSession);
    }
}
