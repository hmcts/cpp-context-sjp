package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.time.LocalDate.now;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSummary;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSearchResultRepository;
import uk.gov.moj.cpp.sjp.persistence.repository.CaseSummaryRepository;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseSearchResultRepositoryTest extends BaseTransactionalTest {

    @Inject
    private CaseSearchResultRepository caseSearchResultRepository;

    @Inject
    private CaseSummaryRepository caseSummaryRepository;

    private static final String URN = "URN";
    private static final String LAST_NAME = "lastName";
    private static final String FIRST_NAME = "firstName";

    @Test
    public void shouldFindByLastName() {

        createCaseSearchResult();

        // check it is case insensitive
        final List<CaseSearchResult> results = caseSearchResultRepository.findByLastName(LAST_NAME.toLowerCase());

        assertThat(results.get(0).getLastName(), equalTo(LAST_NAME));
        assertThat(results.get(0).getCaseSummary().getUrn(), equalTo(URN));
    }

    @Test
    public void shouldFindByLastNameWithOrdering() {

        final CaseSearchResult caseSearchResultA = createCaseSearchResultWithFirstname("firstNameA");
        final CaseSearchResult caseSearchResultB = createCaseSearchResultWithFirstname("firstNameB");

        // check results come back ordered by first name ascending
        final List<CaseSearchResult> results = caseSearchResultRepository.findByLastName(LAST_NAME.toLowerCase());

        assertThat(results.get(0).getLastName(), equalTo(LAST_NAME));
        assertThat(results.get(0).getFirstName(), equalTo(caseSearchResultA.getFirstName()));

        assertThat(results.get(1).getLastName(), equalTo(LAST_NAME));
        assertThat(results.get(1).getFirstName(), equalTo(caseSearchResultB.getFirstName()));
    }

    @Test
    public void shouldFindByUrn() {

        final CaseSearchResult caseSearchResult = createCaseSearchResult();

        // check it is case insensitive
        final List<CaseSearchResult> results = caseSearchResultRepository.findByCaseSummary_urn(URN.toLowerCase());

        assertThat(results.get(0).getLastName(), equalTo(caseSearchResult.getLastName()));
        assertThat(results.get(0).getCaseSummary().getUrn(), equalTo(caseSearchResult.getCaseSummary().getUrn()));
    }

    @Test
    public void shouldFindByCaseId() {

        final CaseSearchResult caseSearchResult = createCaseSearchResult();
        final UUID caseId = caseSearchResult.getCaseId();

        final List<CaseSearchResult> results = caseSearchResultRepository.findByCaseId(caseId);

        assertThat(results.get(0).getLastName(), equalTo(LAST_NAME));
        assertThat(results.get(0).getCaseSummary().getUrn(), equalTo(URN));
        assertThat(results.get(0).getCaseSummary().getId(), equalTo(caseId));

        assertThat(results.size(), is(1));
    }

    @Test
    public void shouldCreateWithoutCaseSummary() {

        final UUID id = UUID.randomUUID();

        final CaseSearchResult caseSearchResult = new CaseSearchResult(id, UUID.randomUUID(),
                "firstName", "lastName", now(), "postCode");

        caseSearchResultRepository.save(caseSearchResult);

        assertThat(caseSearchResultRepository.findBy(id), notNullValue());
    }

    private CaseSearchResult createCaseSearchResult(){
        return createCaseSearchResultWithFirstname(FIRST_NAME);
    }

    private CaseSearchResult createCaseSearchResultWithFirstname(String firstName) {
        CaseSummary caseSummary = new CaseSummary();
        caseSummary.setId(UUID.randomUUID());
        caseSummary.setUrn(URN);
        caseSummary = caseSummaryRepository.save(caseSummary);

        final CaseSearchResult caseSearchResult = new CaseSearchResult();
        caseSearchResult.setId(UUID.randomUUID());
        caseSearchResult.setCaseId(caseSummary.getId());
        caseSearchResult.setCaseSummary(caseSummary);
        caseSearchResult.setFirstName(firstName);
        caseSearchResult.setLastName(LAST_NAME);
        return caseSearchResultRepository.save(caseSearchResult);
    }
}