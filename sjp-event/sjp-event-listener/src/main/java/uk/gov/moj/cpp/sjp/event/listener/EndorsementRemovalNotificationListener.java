package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsFailed;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsGenerated;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsGenerationFailed;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsQueued;
import uk.gov.moj.cpp.sjp.event.NotificationToRemoveEndorsementsSent;
import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus;
import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus.Status;
import uk.gov.moj.cpp.sjp.persistence.repository.EndorsementRemovalNotificationRepository;

import java.time.ZonedDateTime;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class EndorsementRemovalNotificationListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private EndorsementRemovalNotificationRepository repository;

    @Transactional
    @Handles(NotificationToRemoveEndorsementsGenerated.EVENT_NAME)
    public void generated(final JsonEnvelope envelope) {
        final NotificationToRemoveEndorsementsGenerated event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                NotificationToRemoveEndorsementsGenerated.class);

        final NotificationOfEndorsementStatus entity = new NotificationOfEndorsementStatus(
                event.getApplicationDecisionId(),
                event.getFileId(),
                Status.GENERATED,
                ZonedDateTime.now()
        );

        repository.save(entity);
    }

    @Transactional
    @Handles(NotificationToRemoveEndorsementsGenerationFailed.EVENT_NAME)
    public void generationFailed(final JsonEnvelope envelope) {
        final UUID applicationDecisionId = getApplicationDecisionId(envelope);

        NotificationOfEndorsementStatus entity = repository.findBy(applicationDecisionId);

        if (isNull(entity)) {
            entity = new NotificationOfEndorsementStatus(
                    applicationDecisionId,
                    null,
                    Status.GENERATION_FAILED,
                    ZonedDateTime.now()
            );
        } else {
            entity.setStatus(Status.GENERATION_FAILED);
            entity.setUpdated(ZonedDateTime.now());
        }

        repository.save(entity);
    }

    @Transactional
    @Handles(NotificationToRemoveEndorsementsQueued.EVENT_NAME)
    public void notificationQueued(final JsonEnvelope envelope) {
        final UUID applicationDecisionId = getApplicationDecisionId(envelope);
        NotificationOfEndorsementStatus entity = repository.findBy(applicationDecisionId);
        entity.setStatus(Status.QUEUED);
        entity.setUpdated(ZonedDateTime.now());
    }

    @Transactional
    @Handles(NotificationToRemoveEndorsementsFailed.EVENT_NAME)
    public void notificationFailed(final JsonEnvelope envelope) {
        final UUID applicationDecisionId = getApplicationDecisionId(envelope);
        final NotificationOfEndorsementStatus entity = repository.findBy(applicationDecisionId);
        entity.setStatus(Status.FAILED);
        entity.setUpdated(ZonedDateTime.now());
    }

    @Transactional
    @Handles(NotificationToRemoveEndorsementsSent.EVENT_NAME)
    public void notificationSent(final JsonEnvelope envelope) {
        final UUID applicationDecisionId = getApplicationDecisionId(envelope);
        final NotificationOfEndorsementStatus entity = repository.findBy(applicationDecisionId);
        entity.setStatus(Status.SENT);
        entity.setUpdated(ZonedDateTime.now());
    }

    private UUID getApplicationDecisionId(final JsonEnvelope envelope) {
        return UUID.fromString(envelope.payloadAsJsonObject().getString("applicationDecisionId"));
    }
}
