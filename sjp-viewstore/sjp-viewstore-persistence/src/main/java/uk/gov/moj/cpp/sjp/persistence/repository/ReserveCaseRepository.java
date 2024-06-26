package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.ReserveCase;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Query;
import org.apache.deltaspike.data.api.QueryParam;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface ReserveCaseRepository extends EntityRepository<ReserveCase, UUID> {

    List<ReserveCase> findByCaseId(final UUID caseId);

    @Query(value = "SELECT COUNT(rc) FROM ReserveCase rc where rc.reservedBy=:reservedBy")
    int countReservedBy(@QueryParam("reservedBy") UUID reservedBy);

}