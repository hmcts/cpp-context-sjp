package uk.gov.moj.cpp.sjp.persistence.repository;


import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseReferredToCourt;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

public class CaseReferredToCourtRepository {

    @Inject
    private EntityManager em;

    private static final String UNACTIONED_CASES =
            "select cr.case_id, c.urn, sr.first_name, sr.last_name, d.interpreter_language, cr.hearing_date " +
            "from court_referral cr " +
            "join case_details c " +
            "on cr.case_id = c.id " +
            "join case_search_result sr " +
            "on cr.case_id = sr.case_id " +
            "join defendant d " +
            "on cr.case_id = d.case_id and sr.person_id = d.person_id " +
            "where cr.actioned is null " +
            "order by cr.hearing_date";

    public List<CaseReferredToCourt> findUnactionedCases() {
        return em.createNativeQuery(UNACTIONED_CASES, "caseReferredToCourt").getResultList();
    }

}
