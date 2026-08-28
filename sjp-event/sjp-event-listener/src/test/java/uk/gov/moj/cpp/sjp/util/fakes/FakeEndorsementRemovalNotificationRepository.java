package uk.gov.moj.cpp.sjp.util.fakes;

import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus;
import uk.gov.moj.cpp.sjp.persistence.repository.EndorsementRemovalNotificationRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.metamodel.SingularAttribute;

public abstract class FakeEndorsementRemovalNotificationRepository extends EndorsementRemovalNotificationRepository {


    private Map<UUID, NotificationOfEndorsementStatus> inMemoryStorage = new HashMap<>();

    public NotificationOfEndorsementStatus save(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        return this.inMemoryStorage.put(notificationOfEndorsementStatus.getApplicationDecisionId(), clone(notificationOfEndorsementStatus));
    }

    public NotificationOfEndorsementStatus saveAndFlush(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    public NotificationOfEndorsementStatus saveAndFlushAndRefresh(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    public void remove(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    public void removeAndFlush(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    public void attachAndRemove(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    public void refresh(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    public void flush() {
        throw new UnsupportedOperationException();
    }

    public NotificationOfEndorsementStatus findBy(final UUID uuid) {
        return this.inMemoryStorage.get(uuid);
    }

    public Optional<NotificationOfEndorsementStatus> findOptionalBy(final UUID uuid) {
        throw new UnsupportedOperationException();
    }

    public List<NotificationOfEndorsementStatus> findAll() {
        throw new UnsupportedOperationException();
    }

    public List<NotificationOfEndorsementStatus> findAll(final int i, final int i1) {
        throw new UnsupportedOperationException();
    }

    public List<NotificationOfEndorsementStatus> findBy(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    public List<NotificationOfEndorsementStatus> findBy(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final int i, final int i1, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    public List<NotificationOfEndorsementStatus> findByLike(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    public List<NotificationOfEndorsementStatus> findByLike(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final int i, final int i1, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    public Long count() {
        throw new UnsupportedOperationException();
    }

    public Long count(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    public Long countLike(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    public UUID getPrimaryKey(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    private NotificationOfEndorsementStatus clone(final NotificationOfEndorsementStatus entity) {
        return new NotificationOfEndorsementStatus(
                entity.getApplicationDecisionId(),
                entity.getFileId(),
                entity.getStatus(),
                entity.getUpdated()
        );
    }
}
