package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Optional.ofNullable;
import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAdded;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionAccountNumberAddedBdf;
import uk.gov.moj.cpp.sjp.event.FinancialImpositionCorrelationIdAdded;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.repository.DefendantRepository;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;
@SuppressWarnings({"squid:S4114"}) // suppress sonar duplicate method issue due to requirement of BDF.
@ServiceComponent(EVENT_LISTENER)
public class FinancialImpositionExportListener {

    @Inject
    private DefendantRepository defendantRepository;

    private static final String CORRELATION_ID = "correlationId";
    private static final String DEFENDANT_ID = "defendantId";
    private static final String ACCOUNT_NUMBER = "accountNumber";

    @Handles(FinancialImpositionCorrelationIdAdded.EVENT_NAME)
    @Transactional
    public void financialImpositionCorrelationIdAdded(final JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final DefendantDetail defendant = defendantRepository.findBy(fromString(payload.getString(DEFENDANT_ID)));
        ofNullable(defendant).ifPresent(defendantDetail -> {
            defendantDetail.setCorrelationId(fromString(payload.getString(CORRELATION_ID)));
            defendantRepository.save(defendantDetail);
        });
    }

    @Handles(FinancialImpositionAccountNumberAdded.EVENT_NAME)
    @Transactional
    public void financialImpositionAccountNumberAdded(final JsonEnvelope envelope) {
        saveAccountNumber(envelope);
    }

    /**
     * This must remain for rebuild and catchup of events created from the ATCM-6980 BDF
     *
     * @param envelope
     */
    @Handles(FinancialImpositionAccountNumberAddedBdf.EVENT_NAME)
    @Transactional
    public void financialImpositionAccountNumberAddedBdf(final JsonEnvelope envelope) {
        saveCorrelationIdAndAccountNumber(envelope);
    }

    private void saveAccountNumber(JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final DefendantDetail defendant = defendantRepository.findBy(fromString(payload.getString(DEFENDANT_ID)));
        ofNullable(defendant).ifPresent(defendantDetail -> {
            defendantDetail.setAccountNumber(payload.getString(ACCOUNT_NUMBER));
            defendantRepository.save(defendantDetail);
        });
    }

    private void saveCorrelationIdAndAccountNumber(JsonEnvelope envelope) {
        final JsonObject payload = envelope.payloadAsJsonObject();
        final DefendantDetail defendant = defendantRepository.findBy(fromString(payload.getString(DEFENDANT_ID)));
        ofNullable(defendant).ifPresent(defendantDetail -> {
            defendantDetail.setCorrelationId(fromString(payload.getString(CORRELATION_ID)));
            defendantDetail.setAccountNumber(payload.getString(ACCOUNT_NUMBER));
            defendantRepository.save(defendantDetail);
        });
    }

}
