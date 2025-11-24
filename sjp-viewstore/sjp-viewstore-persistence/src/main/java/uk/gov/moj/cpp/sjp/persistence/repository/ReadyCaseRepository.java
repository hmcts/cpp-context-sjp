package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface ReadyCaseRepository extends EntityRepository<ReadyCase, UUID> {

    List<ReadyCase> findByAssigneeId(final UUID assigneeId);

    ReadyCase findByCaseId(final UUID caseId);
}