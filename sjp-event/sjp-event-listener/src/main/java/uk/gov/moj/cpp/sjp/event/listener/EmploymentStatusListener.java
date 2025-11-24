package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.UUID.fromString;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.FinancialMeans;
import uk.gov.moj.cpp.sjp.persistence.repository.FinancialMeansRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class EmploymentStatusListener {

    @Inject
    private FinancialMeansRepository financialMeansRepository;

    @Transactional
    @Handles("sjp.events.employment-status-updated")
    public void updateEmploymentStatus(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final UUID defendantId = fromString(payload.getString("defendantId"));
        final String employmentStatus = payload.getString("employmentStatus");

        FinancialMeans financialMeans = financialMeansRepository.findBy(defendantId);

        if (financialMeans == null) {
            financialMeans = new FinancialMeans(defendantId, employmentStatus);
        } else {
            financialMeans.setEmploymentStatus(employmentStatus);
        }
        financialMeansRepository.save(financialMeans);
    }
}
