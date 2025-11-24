package uk.gov.moj.cpp.sjp.util.fakes;

import uk.gov.moj.cpp.sjp.persistence.entity.NotificationOfEndorsementStatus;
import uk.gov.moj.cpp.sjp.persistence.repository.EndorsementRemovalNotificationRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import javax.persistence.metamodel.SingularAttribute;

public abstract class FakeEndorsementRemovalNotificationRepository implements EndorsementRemovalNotificationRepository {


    private Map<UUID, NotificationOfEndorsementStatus> inMemoryStorage = new HashMap<>();

    @Override
    public NotificationOfEndorsementStatus save(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        return this.inMemoryStorage.put(notificationOfEndorsementStatus.getApplicationDecisionId(), clone(notificationOfEndorsementStatus));
    }

    @Override
    public NotificationOfEndorsementStatus saveAndFlush(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotificationOfEndorsementStatus saveAndFlushAndRefresh(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAndFlush(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attachAndRemove(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(final NotificationOfEndorsementStatus notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NotificationOfEndorsementStatus findBy(final UUID uuid) {
        return this.inMemoryStorage.get(uuid);
    }

    @Override
    public Optional<NotificationOfEndorsementStatus> findOptionalBy(final UUID uuid) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NotificationOfEndorsementStatus> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NotificationOfEndorsementStatus> findAll(final int i, final int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NotificationOfEndorsementStatus> findBy(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NotificationOfEndorsementStatus> findBy(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final int i, final int i1, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NotificationOfEndorsementStatus> findByLike(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<NotificationOfEndorsementStatus> findByLike(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final int i, final int i1, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long countLike(final NotificationOfEndorsementStatus notificationOfEndorsementStatus, final SingularAttribute<NotificationOfEndorsementStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
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
