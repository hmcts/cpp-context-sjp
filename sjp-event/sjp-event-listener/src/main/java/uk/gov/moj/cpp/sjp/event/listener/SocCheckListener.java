package uk.gov.moj.cpp.sjp.event.listener;


import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.SocCheck;
import uk.gov.moj.cpp.sjp.persistence.repository.SocCheckRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_LISTENER)
public class SocCheckListener {

    @Inject
    private SocCheckRepository socCheckRepository;


    @Handles("sjp.events.marked-as-legal-soc-checked")
    public void handleCaseLegalSocChecked(final JsonEnvelope event) {
        final JsonObject payload = event.payloadAsJsonObject();
        final SocCheck socCheck = new SocCheck(UUID.randomUUID(),
                UUID.fromString(payload.getString("caseId")),
                UUID.fromString(payload.getString("checkedBy")),
                ZonedDateTime.parse(payload.getString("checkedAt"))
        );

        socCheckRepository.save(socCheck);
    }
}
