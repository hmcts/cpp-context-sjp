package uk.gov.moj.cpp.sjp.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;
import uk.gov.moj.cpp.sjp.persistence.entity.ReserveCase;

@Repository
public abstract class ReserveCaseRepository implements EntityRepository<ReserveCase, UUID> {

    public abstract List<ReserveCase> findByCaseId(final UUID caseId);

}