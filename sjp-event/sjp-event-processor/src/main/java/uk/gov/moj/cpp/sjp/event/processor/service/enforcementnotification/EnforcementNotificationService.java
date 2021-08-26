package uk.gov.moj.cpp.sjp.event.processor.service.enforcementnotification;

import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.json.schemas.domains.sjp.queries.Defendant;
import uk.gov.justice.services.core.annotation.FrameworkComponent;
import uk.gov.justice.services.core.enveloper.Enveloper;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.domain.EnforcementPendingApplicationRequiredNotification;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.UUID;

import javax.inject.Inject;

public class EnforcementNotificationService {

    static final String ENFORCEMENT_PENDING_APPLICATION_CHECK_REQUIRES_NOTIFICATION_COMMAND = "sjp.command.enforcement-pending-application-check-requires-notification";

    @Inject
    private LastDecisionHelper lastDecisionHelper;

    @Inject
    private DivisionCodeHelper divisionCodeHelper;

    @Inject
    private FinancialImpositionHelper financialImpositionHelper;

    @Inject
    private SjpService sjpService;


    @Inject
    @FrameworkComponent(EVENT_PROCESSOR)
    private Sender sender;

    @Inject
    private Enveloper enveloper;

    public void checkIfEnforcementToBeNotified(UUID caseId, JsonEnvelope jsonEnvelope) {
        final CaseDetails caseDetails = sjpService.getCaseDetails(caseId, jsonEnvelope);
        if (caseDetails != null) {
            final Defendant defendant = caseDetails.getDefendant();
            final String postcode = defendant.getPersonalDetails().getAddress().getPostcode();
            final int divisionCode = divisionCodeHelper.divisionCode(jsonEnvelope, caseDetails, postcode);

            final EnforcementPendingApplicationRequiredNotification initiateNotificationPayload
                    = new EnforcementPendingApplicationRequiredNotification(caseDetails.getId(), divisionCode);

            final Envelope<EnforcementPendingApplicationRequiredNotification> envelope = envelop(initiateNotificationPayload)
                    .withName(ENFORCEMENT_PENDING_APPLICATION_CHECK_REQUIRES_NOTIFICATION_COMMAND)
                    .withMetadataFrom(jsonEnvelope);
            sender.send(envelope);
        }
    }
}