package uk.gov.moj.cpp.sjp.util.fakes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.metamodel.SingularAttribute;
import uk.gov.moj.cpp.sjp.persistence.entity.AocpAcceptedEmailStatus;
import uk.gov.moj.cpp.sjp.persistence.repository.AocpAcceptedEmailNotificationStatusRepository;

public abstract class FakeAocpAcceptedEmailNotificationRepository extends AocpAcceptedEmailNotificationStatusRepository {


    private Map<UUID, AocpAcceptedEmailStatus> inMemoryStorage = new HashMap<>();

    public AocpAcceptedEmailStatus save(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        return this.inMemoryStorage.put(notificationOfAocpAcceptedEmail.getCaseId(), clone(notificationOfAocpAcceptedEmail));
    }

    public AocpAcceptedEmailStatus saveAndFlush(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    public AocpAcceptedEmailStatus saveAndFlushAndRefresh(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    public void remove(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    public void removeAndFlush(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    public void attachAndRemove(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    public void refresh(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail) {
        throw new UnsupportedOperationException();
    }

    public void flush() {
        throw new UnsupportedOperationException();
    }

    public AocpAcceptedEmailStatus findBy(final UUID uuid) {
        return this.inMemoryStorage.get(uuid);
    }

    public List<AocpAcceptedEmailStatus> findAll() {
        throw new UnsupportedOperationException();
    }

    public List<AocpAcceptedEmailStatus> findAll(final int i, final int i1) {
        throw new UnsupportedOperationException();
    }

    public List<AocpAcceptedEmailStatus> findBy(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    public List<AocpAcceptedEmailStatus> findBy(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final int i, final int i1, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    public List<AocpAcceptedEmailStatus> findByLike(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    public List<AocpAcceptedEmailStatus> findByLike(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final int i, final int i1, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    public Long count() {
        throw new UnsupportedOperationException();
    }

    public Long count(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

    public Long countLike(final AocpAcceptedEmailStatus notificationOfAocpAcceptedEmail, final SingularAttribute<AocpAcceptedEmailStatus, ?>... singularAttributes) {
        throw new UnsupportedOperationException();
    }

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
