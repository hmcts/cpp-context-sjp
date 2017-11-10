package uk.gov.moj.cpp.sjp.persistence.repository;


import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseCountByAgeView;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;

public class NotReadyCaseRepository {

    @Inject
    private EntityManager em;

    // Considering that cases may have multiple defendants and offences
    // There is no index on structure_offence.pending_withdrawal as it will only be true infrequently
    private static final String COUNT_OF_CASES_BY_AGE_QUERY = "select current_date - posting_date as age, count (*) as count from " +
            "(select distinct c.id, c.posting_date " +
            "from case_details c " +
            "join defendant d on c.id = d.case_id " +
            "join offence o on d.id = o.defendant_id " +
            "where completed is false and initiation_code = 'J' " +
            "and c.posting_date > current_date - 28 " +
            "and o.plea is null and o.pending_withdrawal is not true) as case_posting_dates " +
            "group by age order by age";

    public List<CaseCountByAgeView> getCountOfCasesByAge() {
        return em.createNativeQuery(COUNT_OF_CASES_BY_AGE_QUERY, "caseCountByAge").getResultList();
    }

}
