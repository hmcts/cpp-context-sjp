package uk.gov.moj.cpp.sjp.event.listener;


import static uk.gov.justice.services.core.annotation.Component.EVENT_LISTENER;


import java.time.ZonedDateTime;
import javax.inject.Inject;
import javax.transaction.Transactional;
import uk.gov.justice.services.common.converter.JsonObjectToObjectConverter;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationFailed;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationQueued;
import uk.gov.moj.cpp.sjp.event.AocpAcceptedEmailNotificationSent;
import uk.gov.moj.cpp.sjp.persistence.entity.AocpAcceptedEmailStatus;
import uk.gov.moj.cpp.sjp.persistence.repository.AocpAcceptedEmailNotificationStatusRepository;

@ServiceComponent(EVENT_LISTENER)
public class AocpAcceptedEmailNotificationListener {

    @Inject
    private JsonObjectToObjectConverter jsonObjectToObjectConverter;
    @Inject
    private AocpAcceptedEmailNotificationStatusRepository repository;

    @Transactional
    @Handles(AocpAcceptedEmailNotificationQueued.EVENT_NAME)
    public void notificationQueued(final JsonEnvelope envelope) {
        final AocpAcceptedEmailNotificationQueued event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                AocpAcceptedEmailNotificationQueued.class);
        final AocpAcceptedEmailStatus entity = new AocpAcceptedEmailStatus();
        entity.setCaseId(event.getCaseId());
        entity.setStatus(AocpAcceptedEmailStatus.Status.QUEUED);
        entity.setUpdated(ZonedDateTime.now());
        repository.save(entity);
    }

    @Transactional
    @Handles(AocpAcceptedEmailNotificationFailed.EVENT_NAME)
    public void notificationFailed(final JsonEnvelope envelope) {
        final AocpAcceptedEmailNotificationFailed event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                AocpAcceptedEmailNotificationFailed.class);
        final AocpAcceptedEmailStatus entity = repository.findBy(event.getCaseId());
        entity.setStatus(AocpAcceptedEmailStatus.Status.FAILED);
        entity.setUpdated(ZonedDateTime.now());
        repository.save(entity);
    }

    @Transactional
    @Handles(AocpAcceptedEmailNotificationSent.EVENT_NAME)
    public void notificationSent(final JsonEnvelope envelope) {
        final AocpAcceptedEmailNotificationSent event = jsonObjectToObjectConverter.convert(
                envelope.payloadAsJsonObject(),
                AocpAcceptedEmailNotificationSent.class);
        final AocpAcceptedEmailStatus entity = repository.findBy(event.getCaseId());
        entity.setStatus(AocpAcceptedEmailStatus.Status.SENT);
        entity.setUpdated(ZonedDateTime.now());
        repository.save(entity);
    }

}
