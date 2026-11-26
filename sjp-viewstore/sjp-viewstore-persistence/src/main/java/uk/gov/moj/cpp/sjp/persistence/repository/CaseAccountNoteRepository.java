package uk.gov.moj.cpp.sjp.persistence.repository;

import uk.gov.moj.cpp.sjp.persistence.entity.AccountNote;

import javax.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class CaseAccountNoteRepository extends JPAEntityRepository<AccountNote, UUID> {

    @Override
    protected Class<AccountNote> getEntityClass() {
        return AccountNote.class;
    }

    public List<AccountNote> findByCaseUrn(final String caseUrn) {
        return entityManager.createQuery("SELECT e FROM AccountNote e WHERE e.caseUrn = :caseUrn", AccountNote.class)
                .setParameter("caseUrn", caseUrn)
                .getResultList();
    }
}