package uk.gov.moj.cpp.sjp.event.processor;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.messaging.JsonEnvelope.envelopeFrom;
import static uk.gov.justice.services.messaging.JsonEnvelope.metadataFrom;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.fileservice.api.FileServiceException;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.decision.ApplicationDecisionSetAside;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;
import uk.gov.moj.cpp.sjp.event.processor.service.models.CaseDetailsDecorator;
import uk.gov.moj.cpp.sjp.event.processor.service.notification.EndorsementRemovalNotificationService;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class ApplicationSetAsideProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationSetAsideProcessor.class);
    private static final String PUBLIC_APPLICATION_SET_ASIDE_EVENT = "public.sjp.application-decision-set-aside";

    @Inject
    private JsonObjectToObjectConverter converter;
    @ServiceComponent(EVENT_PROCESSOR)
    @Inject
    private Sender sender;
    @Inject
    private SjpService sjpService;
    @Inject
    private EndorsementRemovalNotificationService endorsementRemovalNotificationService;

    @Handles(ApplicationDecisionSetAside.EVENT_NAME)
    public void handleApplicationDecisionSetAside(final JsonEnvelope envelope) throws FileServiceException {
        sendApplicationSetAsidePublicEvent(envelope);
        sendNotificationToDvlaToRemoveEndorsements(envelope);
    }

    private void sendApplicationSetAsidePublicEvent(final JsonEnvelope envelope) {
        LOGGER.info("Sending public.sjp.application-decision-set-aside for caseId: {}", envelope.payloadAsJsonObject().getString("caseId"));
        sender.send(envelopeFrom(metadataFrom(envelope.metadata()).withName(PUBLIC_APPLICATION_SET_ASIDE_EVENT), envelope.payload()));
    }

    private void sendNotificationToDvlaToRemoveEndorsements(final JsonEnvelope envelope) throws FileServiceException {
        final ApplicationDecisionSetAside decision = converter.convert(envelope.payloadAsJsonObject(), ApplicationDecisionSetAside.class);
        final CaseDetails caseDetails = sjpService.getCaseDetails(decision.getCaseId(), envelope);
        final CaseDetailsDecorator caseDetailsDecorator = new CaseDetailsDecorator(caseDetails);

        if (endorsementRemovalNotificationService.hasEndorsementsToBeRemoved(caseDetailsDecorator)) {
            LOGGER.info("Case has endorsements to be removed. Preparing notification to DVLA for caseId: {}", decision.getCaseId());
            endorsementRemovalNotificationService.generateNotification(caseDetailsDecorator, envelope);
        } else {
            LOGGER.info("Case noes not have endorsements to be removed for caseId: {}", decision.getCaseId());
        }
    }
}
