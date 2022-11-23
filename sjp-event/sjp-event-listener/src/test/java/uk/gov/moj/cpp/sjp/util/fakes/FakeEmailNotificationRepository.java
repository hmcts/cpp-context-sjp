package uk.gov.moj.cpp.sjp.util.fakes;

import static java.util.UUID.randomUUID;

import uk.gov.moj.cpp.sjp.persistence.entity.EmailNotification;
import uk.gov.moj.cpp.sjp.persistence.repository.EmailNotificationRepository;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.metamodel.SingularAttribute;

public class FakeEmailNotificationRepository implements EmailNotificationRepository {

    private Map<UUID, EmailNotification> inMemoryStorage = new HashMap<>();

    @Override
    public EmailNotification save(final EmailNotification emailNotification) {
        return this.inMemoryStorage.put(emailNotification.getId(), clone(emailNotification));
    }

    @Override
    public EmailNotification saveAndFlush(final EmailNotification emailNotification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EmailNotification saveAndFlushAndRefresh(final EmailNotification emailNotification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(final EmailNotification emailNotification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAndFlush(final EmailNotification emailNotification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attachAndRemove(final EmailNotification emailNotification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(final EmailNotification emailNotification) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EmailNotification findBy(final UUID uuid) {
        return this.inMemoryStorage.get(uuid);
    }

    @Override
    public List<EmailNotification> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EmailNotification> findAll(final int i, final int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EmailNotification> findBy(final EmailNotification emailNotification, final SingularAttribute<EmailNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EmailNotification> findBy(final EmailNotification emailNotification, final int i, final int i1, final SingularAttribute<EmailNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EmailNotification> findByLike(final EmailNotification emailNotification, final SingularAttribute<EmailNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EmailNotification> findByLike(final EmailNotification emailNotification, final int i, final int i1, final SingularAttribute<EmailNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count(final EmailNotification emailNotification, final SingularAttribute<EmailNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long countLike(final EmailNotification emailNotification, final SingularAttribute<EmailNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UUID getPrimaryKey(final EmailNotification emailNotification) {
        throw new UnsupportedOperationException();
    }

    private EmailNotification clone(final EmailNotification entity) {
        return new EmailNotification(
                randomUUID(),
                entity.getReferenceId(),
                entity.getStatus(),
                entity.getUpdated(),
                entity.getNotificationType()
        );
    }

    @Override
    public EmailNotification findByReferenceIdAndNotificationType(final UUID referenceId, final EmailNotification.NotificationNotifyDocumentType notificationType) {
        Iterator it = this.inMemoryStorage.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            EmailNotification emailNotification = (EmailNotification) pair.getValue();

            if(referenceId.equals(emailNotification.getReferenceId()) && notificationType.equals(emailNotification.getNotificationType())){
                return emailNotification;
            }
        }

        return null;
    }
}
