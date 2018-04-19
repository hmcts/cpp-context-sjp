package uk.gov.moj.cpp.sjp.persistence.repository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;
import uk.gov.moj.cpp.sjp.persistence.entity.view.ReadyCasesReasonCount;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public abstract class ReadyCasesRepository implements EntityRepository<ReadyCase, UUID> {

    @Inject
    private EntityManager em;

    public List<ReadyCasesReasonCount> getReadyCasesReasonCount() {
        return em.createNamedQuery("readyCases.readyCasesReasonCounts").getResultList();
    }
}