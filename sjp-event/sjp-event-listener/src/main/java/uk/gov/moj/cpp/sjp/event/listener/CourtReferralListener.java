package uk.gov.moj.cpp.sjp.event.listener;

import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.persistence.entity.CourtReferral;
import uk.gov.moj.cpp.sjp.persistence.repository.CourtReferralRepository;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

@ServiceComponent(EVENT_LISTENER)
public class CourtReferralListener {

    @Inject
    private CourtReferralRepository repository;

    @Handles("sjp.events.court-referral-created")
    public void courtReferralCreated(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        final CourtReferral courtReferral = new CourtReferral(
                UUID.fromString(payload.getString("caseId")),
                LocalDate.parse(payload.getString("hearingDate")));
        repository.save(courtReferral);
    }

    @Handles("sjp.events.court-referral-actioned")
    public void courtReferralActioned(final JsonEnvelope envelope) {

        final JsonObject payload = envelope.payloadAsJsonObject();

        final CourtReferral courtReferral = repository.findBy(UUID.fromString(payload.getString("caseId")));
        courtReferral.setActioned(ZonedDateTime.parse(payload.getString("actioned")));
        repository.save(courtReferral);
    }
}
