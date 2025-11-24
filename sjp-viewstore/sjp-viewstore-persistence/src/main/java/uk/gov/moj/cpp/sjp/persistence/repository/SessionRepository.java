package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.Session;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface SessionRepository extends EntityRepository<Session, UUID> {

    @Query(value = "select session1 from Session session1 where session1.type='AOCP' and session1.endedAt is null and session1.startedAt =" +
            "(select max(session2.startedAt) from Session session2 where session2.type='AOCP' and session2.endedAt is null)")
    List<Session> findLatestAocpSession();

}
