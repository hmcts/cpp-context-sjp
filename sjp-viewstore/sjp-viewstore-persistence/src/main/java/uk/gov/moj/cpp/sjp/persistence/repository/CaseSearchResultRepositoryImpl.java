package uk.gov.moj.cpp.sjp.persistence.repository;


import static java.util.UUID.fromString;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;

public class CaseSearchResultRepositoryImpl {

    @Inject
    private EntityManager em;

    public void removePersonInfo(final String mappingId) {
        final Query query = em.createNativeQuery("DELETE FROM case_search_result WHERE id = :p");
        query.setParameter("p", fromString(mappingId));
        query.executeUpdate();
    }

    public EntityManager getEm() {
        return em;
    }

    public void setEm(final EntityManager em) {
        this.em = em;
    }
}
