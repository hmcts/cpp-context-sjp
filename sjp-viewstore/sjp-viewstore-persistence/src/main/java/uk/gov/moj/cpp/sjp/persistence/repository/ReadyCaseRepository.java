package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public abstract class ReadyCaseRepository implements EntityRepository<ReadyCase, UUID> {

    public abstract List<ReadyCase> findByAssigneeId(final UUID assigneeId);
}