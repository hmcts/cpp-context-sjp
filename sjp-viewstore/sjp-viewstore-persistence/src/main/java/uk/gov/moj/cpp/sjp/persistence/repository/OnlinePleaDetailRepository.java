package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.OnlinePleaDetail;

import java.util.List;
import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public abstract class OnlinePleaDetailRepository implements EntityRepository<OnlinePleaDetail, UUID> {

    public abstract List<OnlinePleaDetail> findByCaseIdAndDefendantId(final UUID caseId, final UUID defendantId);
}
