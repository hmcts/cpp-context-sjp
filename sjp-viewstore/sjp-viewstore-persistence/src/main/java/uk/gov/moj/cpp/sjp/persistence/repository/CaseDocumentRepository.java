package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

import javax.enterprise.context.ApplicationScoped;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CaseDocumentRepository extends JPAEntityRepository<CaseDocument, UUID> {

    @Override
    protected Class<CaseDocument> getEntityClass() {
        return CaseDocument.class;
    }

    public List<CaseDocument> findCaseDocumentsOrderedByAddedByDescending(
            final ZonedDateTime fromDate,
            final ZonedDateTime toDate,
            final String documentType) {
        return entityManager.createQuery("SELECT cd FROM CaseDocument cd WHERE cd.documentType = :documentType AND cd.addedAt >= :fromDate AND cd.addedAt < :toDate ORDER BY cd.addedAt DESC", CaseDocument.class)
                .setParameter("fromDate", fromDate)
                .setParameter("toDate", toDate)
                .setParameter("documentType", documentType)
                .getResultList();
    }

    public CaseDocument findByMaterialId(final UUID materialId) {
        return entityManager.createQuery("SELECT cd FROM CaseDocument cd WHERE cd.materialId = :materialId", CaseDocument.class)
                .setParameter("materialId", materialId)
                .getResultStream().findFirst().orElse(null);
    }
}
