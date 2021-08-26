package uk.gov.moj.cpp.sjp.util.fakes;

import uk.gov.moj.cpp.sjp.persistence.entity.EnforcementNotification;
import uk.gov.moj.cpp.sjp.persistence.repository.EnforcementPendingApplicationNotificationStatusRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.metamodel.SingularAttribute;

public class FakeEnforcementPendingApplicationNotificationRepository implements EnforcementPendingApplicationNotificationStatusRepository {


    private Map<UUID, EnforcementNotification> inMemoryStorage = new HashMap<>();

    @Override
    public EnforcementNotification save(final EnforcementNotification notificationOfEndorsementStatus) {
        return this.inMemoryStorage.put(notificationOfEndorsementStatus.getApplicationId(), clone(notificationOfEndorsementStatus));
    }

    @Override
    public EnforcementNotification saveAndFlush(final EnforcementNotification notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public EnforcementNotification saveAndFlushAndRefresh(final EnforcementNotification notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(final EnforcementNotification notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAndFlush(final EnforcementNotification notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attachAndRemove(final EnforcementNotification notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(final EnforcementNotification notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    @Override
    public EnforcementNotification findBy(final UUID uuid) {
        return this.inMemoryStorage.get(uuid);
    }

    @Override
    public List<EnforcementNotification> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EnforcementNotification> findAll(final int i, final int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EnforcementNotification> findBy(final EnforcementNotification notificationOfEndorsementStatus, final SingularAttribute<EnforcementNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EnforcementNotification> findBy(final EnforcementNotification notificationOfEndorsementStatus, final int i, final int i1, final SingularAttribute<EnforcementNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EnforcementNotification> findByLike(final EnforcementNotification notificationOfEndorsementStatus, final SingularAttribute<EnforcementNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<EnforcementNotification> findByLike(final EnforcementNotification notificationOfEndorsementStatus, final int i, final int i1, final SingularAttribute<EnforcementNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count(final EnforcementNotification notificationOfEndorsementStatus, final SingularAttribute<EnforcementNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long countLike(final EnforcementNotification notificationOfEndorsementStatus, final SingularAttribute<EnforcementNotification, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UUID getPrimaryKey(final EnforcementNotification notificationOfEndorsementStatus) {
        throw new UnsupportedOperationException();
    }

    private EnforcementNotification clone(final EnforcementNotification entity) {
        return new EnforcementNotification(
                entity.getApplicationId(),
                entity.getFileId(),
                entity.getStatus(),
                entity.getUpdated()
        );
    }
}
