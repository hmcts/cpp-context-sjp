package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class CaseDocumentRepository {

    @PersistenceContext(unitName = "sjp-persistence-unit")
    private EntityManager entityManager;

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
                .getSingleResult();
    }

    public CaseDocument findBy(final UUID id) {
        return entityManager.find(CaseDocument.class, id);
    }

    public CaseDocument save(final CaseDocument entity) {
        return entityManager.merge(entity);
    }

    public void remove(final CaseDocument entity) {
        entityManager.remove(entityManager.contains(entity) ? entity : entityManager.merge(entity));
    }

    public Long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM CaseDocument e", Long.class).getSingleResult();
    }

    public List<CaseDocument> findAll() {
        return entityManager.createQuery("SELECT e FROM CaseDocument e", CaseDocument.class).getResultList();
    }
}
