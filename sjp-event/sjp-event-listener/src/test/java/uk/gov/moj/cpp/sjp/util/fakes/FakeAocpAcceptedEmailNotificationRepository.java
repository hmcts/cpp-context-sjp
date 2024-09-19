package uk.gov.moj.cpp.sjp.util.fakes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.persistence.metamodel.SingularAttribute;
import uk.gov.moj.cpp.sjp.persistence.entity.AocpAcceptedEmailStatus;
import uk.gov.moj.cpp.sjp.persistence.repository.AocpAcceptedEmailNotificationStatusRepository;

public abstract class FakeAocpAcceptedEmailNotificationRepository implements AocpAcceptedEmailNotificationStatusRepository {


    private Map<UUID, AocpAcceptedEmailStatus> inMemoryStorage = new HashMap<>();

    @Override
    public AocpAcceptedEmailStatus save(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        return this.inMemoryStorage.put(notificationOfAocpAcceptedEmail.getCaseId(), clone(notificationOfAocpAcceptedEmail));
    }

    @Override
    public AocpAcceptedEmailStatus saveAndFlush(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public AocpAcceptedEmailStatus saveAndFlushAndRefresh(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remove(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAndFlush(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void attachAndRemove(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void refresh(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AocpAcceptedEmailStatus findBy(final UUID uuid) {
        return this.inMemoryStorage.get(uuid);
    }

    @Override
    public List<AocpAcceptedEmailStatus> findAll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AocpAcceptedEmailStatus> findAll(final int i, final int i1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AocpAcceptedEmailStatus> findBy(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AocpAcceptedEmailStatus> findBy(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final int i, final int i1, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AocpAcceptedEmailStatus> findByLike(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<AocpAcceptedEmailStatus> findByLike(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final int i, final int i1, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long count(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Long countLike(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    @Override
    public UUID getPrimaryKey(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    private AocpAcceptedEmailStatus clone(final AocpAcceptedEmailStatus entity) {
        return new AocpAcceptedEmailStatus(
                entity.getCaseId(),
                entity.getStatus(),
                entity.getUpdated()
        );
    }
}
