package uk.gov.moj.cpp.sjp.event.listener;

import static java.util.Objects.isNull;
import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;

import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationFailed;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationGenerated;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationGenerationFailed;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationRequired;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationQueued;
import uk.gov.moj.cpp.sjp.event.EnforcementPendingApplicationNotificationSent;
import uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification;
import uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification.Status;
import uk.gov.moj.cpp.sjp.persistence.repository.EnforcementPendingApplicationNotificationStatusRepository;

import java.time.ZonedDateTime;

import javax.inject.Inject;
import javax.transaction.Transactional;

@ServiceComponent(EVENT_LISTENER)
public class EnforcementPendingApplicationNotificationListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private EnforcementPendingApplicationNotificationStatusRepository repository;

    @Transactional
    @Handles(EnforcementPendingApplicationNotificationRequired.EVENT_NAME)
    public void required(final JsonEnvelope envelope) {
        final EnforcementPendingApplicationNotificationRequired event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                EnforcementPendingApplicationNotificationRequired.class);

        final EnforcementNotification entity = new EnforcementNotification(
                event.getApplicationId(), null, Status.REQUIRED, ZonedDateTime.now()
        );
        repository.save(entity);
    }

    @Transactional
    @Handles(EnforcementPendingApplicationNotificationGenerated.EVENT_NAME)
    @SuppressWarnings("squid:S1854")
    public void generated(final JsonEnvelope envelope) {

        final EnforcementPendingApplicationNotificationGenerated event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                EnforcementPendingApplicationNotificationGenerated.class);

        EnforcementNotification entity = repository.findBy(event.getApplicationId());

        if (isNull(entity)) {
            entity = new EnforcementNotification(
                    event.getApplicationId(), null,
                    EnforcementNotification.Status.NOT_INITIATED,
                    ZonedDateTime.now());
        } else {
            entity.setFileId(event.getFileId());
            entity.setStatus(Status.GENERATED);
            entity.setUpdated(ZonedDateTime.now());
        }
        repository.save(entity);
    }

    @Transactional
    @Handles(EnforcementPendingApplicationNotificationGenerationFailed.EVENT_NAME)
    public void generationFailed(final JsonEnvelope envelope) {
        final EnforcementPendingApplicationNotificationGenerationFailed event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                EnforcementPendingApplicationNotificationGenerationFailed.class);

        EnforcementNotification entity = repository.findBy(event.getApplicationId());

        if (isNull(entity)) {
            entity = new EnforcementNotification(
                    event.getApplicationId(),
                    null,
                    EnforcementNotification.Status.GENERATION_FAILED,
                    ZonedDateTime.now()
            );
        } else {
            entity.setStatus(EnforcementNotification.Status.GENERATION_FAILED);
            entity.setUpdated(ZonedDateTime.now());
        }
        repository.save(entity);
    }

    @Transactional
    @Handles(EnforcementPendingApplicationNotificationQueued.EVENT_NAME)
    public void notificationQueued(final JsonEnvelope envelope) {
        final EnforcementPendingApplicationNotificationQueued event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                EnforcementPendingApplicationNotificationQueued.class);
        final EnforcementNotification entity = repository.findBy(event.getApplicationId());
        entity.setStatus(Status.QUEUED);
        entity.setUpdated(ZonedDateTime.now());
        repository.save(entity);
    }

    @Transactional
    @Handles(EnforcementPendingApplicationNotificationFailed.EVENT_NAME)
    public void notificationFailed(final JsonEnvelope envelope) {
        final EnforcementPendingApplicationNotificationFailed event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                EnforcementPendingApplicationNotificationFailed.class);
        final EnforcementNotification entity = repository.findBy(event.getApplicationId());
        entity.setStatus(Status.FAILED);
        entity.setUpdated(ZonedDateTime.now());
        repository.save(entity);
    }

    @Transactional
    @Handles(EnforcementPendingApplicationNotificationSent.EVENT_NAME)
    public void notificationSent(final JsonEnvelope envelope) {
        final EnforcementPendingApplicationNotificationSent event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                EnforcementPendingApplicationNotificationSent.class);
        final EnforcementNotification entity = repository.findBy(event.getApplicationId());
        entity.setStatus(EnforcementNotification.Status.SENT);
        entity.setUpdated(ZonedDateTime.now());
        repository.save(entity);
    }

}
