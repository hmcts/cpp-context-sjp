package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseRepository;

import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class DatesToAvoidUpdatedListener {

    @Inject
    private CaseRepository caseRepository;

    @Transactional
    @Handles("sjp.events.dates-to-avoid-updated")
    public void updateDatesToAvoid(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final UUID caseId = UUID.fromString(payload.getString("caseId"));
        final String datesToAvoid = payload.getString("datesToAvoid");

        caseRepository.updateDatesToAvoid(caseId, datesToAvoid);
    }

}
