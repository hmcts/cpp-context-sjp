package uk.gov.moj.cpp.sjp.event.processor;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_PROCESSOR;

import uk.gov.justice.json.schemas.domains.sjp.queries.CaseDetails;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.processor.service.SjpService;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(EVENT_PROCESSOR)
public class StagingEnforcementEventProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StagingEnforcementEventProcessor.class);

    private static final String ATCM_ORIGINATOR_IDENTIFIER = "ATCM";
    private static final String ACKNOWLEDGEMENT = "acknowledgement";
    private static final String ACCOUNT_NUMBER = "accountNumber";

    @Inject
    private SjpService sjpService;

    @Handles("public.stagingenforcement.enforce-financial-imposition-acknowledgement")
    public void financialImpositionEnforced(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();

        if (isForATCM(payload)) {
            final UUID correlationId = fromString(payload.getString("requestId"));

            final Optional<CaseDetails> caseDetailsOptional = ofNullable(sjpService.getCaseDetailsByCorrelationId(correlationId, event));
            caseDetailsOptional.ifPresent(caseDetails -> addAccountNumber(caseDetails, correlationId, event));
            if(!caseDetailsOptional.isPresent()) {
                LOGGER.warn("no case found for correlationId {}", correlationId);
            }
        } else {
            LOGGER.debug("Request skipped as request originator {} != {}",
                    ATCM_ORIGINATOR_IDENTIFIER,
                    payload.getString("originator", null));
        }
    }

    private void addAccountNumber(final CaseDetails caseDetails, final UUID correlationId,  final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        if(payload.containsKey(ACKNOWLEDGEMENT) &&
                payload.getJsonObject(ACKNOWLEDGEMENT).containsKey(ACCOUNT_NUMBER)) {
            final String accountNumber = payload.getJsonObject(ACKNOWLEDGEMENT)
                    .getString(ACCOUNT_NUMBER);

            sjpService.addAccountNumberToDefendant(caseDetails.getId(), correlationId, accountNumber, event);

        } else {
            LOGGER.error("enforce-financial-imposition-acknowledgement doesn't contain acknowledgment");
        }
    }

    private static boolean isForATCM(final JsonObject payload) {
        return ATCM_ORIGINATOR_IDENTIFIER.equalsIgnoreCase(payload.getString("originator"));
    }

}
