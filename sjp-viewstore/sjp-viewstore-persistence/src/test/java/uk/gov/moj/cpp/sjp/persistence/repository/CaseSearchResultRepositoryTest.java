package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSummary;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private static final String LAST_NAME_UPDATED = "updatedFamilyName";
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
    public void shouldFindByLastNameEvenThoughItWasChanged() {
        // given
        CaseSearchResult caseSearchResult = createCaseSearchResult();
        // when
        final List<CaseSearchResult> results = caseSearchResultRepository.findByLastName(LAST_NAME.toLowerCase());
        // then
        assertThat(results, hasSize(1));

        // given
        simulateDefendantLastNameChanged(LAST_NAME_UPDATED, results.get(0));
        // when
        final List<CaseSearchResult> afterUpdateForOldLastName = caseSearchResultRepository.findByLastName(LAST_NAME.toLowerCase());
        final List<CaseSearchResult> afterUpdateForNewLastName = caseSearchResultRepository.findByLastName(LAST_NAME_UPDATED.toLowerCase());
        // then
        assertThat(afterUpdateForOldLastName, hasSize(1));
        assertThat(afterUpdateForOldLastName.get(0).getCurrentLastName(), equalTo(LAST_NAME_UPDATED));
        assertThat(afterUpdateForNewLastName, hasSize(1));
        assertThat(afterUpdateForNewLastName.get(0).getCurrentLastName(), equalTo(LAST_NAME_UPDATED));

        // given
        simulateDefendantLastNameChanged(LAST_NAME, caseSearchResultRepository.findByCaseId(caseSearchResult.getCaseId()));
        // when
        final List<CaseSearchResult> afterUpdateBackForOriginalLastName = caseSearchResultRepository.findByLastName(LAST_NAME.toLowerCase());
        final List<CaseSearchResult> afterUpdateBackForUpdatedButRevertedLastName = caseSearchResultRepository.findByLastName(LAST_NAME_UPDATED.toLowerCase());
        // then
        assertThat(afterUpdateBackForOriginalLastName, hasSize(1));
        assertThat(afterUpdateBackForOriginalLastName.get(0).getCurrentLastName(), equalTo(LAST_NAME));
        assertThat(afterUpdateBackForUpdatedButRevertedLastName, hasSize(1));
        assertThat(afterUpdateBackForUpdatedButRevertedLastName.get(0).getCurrentLastName(), equalTo(LAST_NAME));

        // given last name was updated to LAST_NAME_UPDATED and a new case with LAST_NAME was created
        simulateDefendantLastNameChanged(LAST_NAME_UPDATED, caseSearchResultRepository.findByCaseSummary_urn(URN));
        createCaseSearchResult();
        // when
        final List<CaseSearchResult> resultsAfterInsert = caseSearchResultRepository.findByLastName(LAST_NAME.toLowerCase());
        // then the results should contain 1 entry with LAST_NAME and 1 entry with LAST_NAME_UPDATED
        assertThat(resultsAfterInsert.stream()
                .map(CaseSearchResult::getCurrentLastName).collect(Collectors.toList()),
                contains(LAST_NAME_UPDATED, LAST_NAME));
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
        final CaseSearchResult caseSearchResult = new CaseSearchResult(UUID.randomUUID(), "firstName", "lastName", LocalDates.from("2001-02-03"));

        caseSearchResultRepository.save(caseSearchResult);

        assertThat(caseSearchResultRepository.findBy(caseSearchResult.getId()), notNullValue());
    }

    private CaseSearchResult createCaseSearchResult() {
        return createCaseSearchResultWithFirstname(FIRST_NAME);
    }

    private CaseSearchResult createCaseSearchResultWithFirstname(String firstName) {
        return createCaseSearchResultWith(UUID.randomUUID(), URN, firstName, LAST_NAME);
    }

    private void simulateDefendantLastNameChanged(String newLastName, CaseSearchResult... caseSearchResults) {
        simulateDefendantLastNameChanged(newLastName, Arrays.asList(caseSearchResults));
    }

    private void simulateDefendantLastNameChanged(String newLastName, List<CaseSearchResult> caseSearchResults) {
        caseSearchResults.forEach(r -> {
            r.setCurrentLastName(newLastName);
            r.setDeprecated(true);
            caseSearchResultRepository.saveAndFlush(r);
        });

        createCaseSearchResultWith(
                caseSearchResults.get(0).getCaseId(),
                caseSearchResults.get(0).getCaseSummary().getUrn(),
                FIRST_NAME,
                newLastName);
    }

    private CaseSearchResult createCaseSearchResultWith(
            UUID caseId, String urn, String firstName, String lastName) {
        CaseSummary caseSummary = new CaseSummary();
        caseSummary.setId(caseId);
        caseSummary.setUrn(urn);
        caseSummary = caseSummaryRepository.save(caseSummary);

        final CaseSearchResult caseSearchResult = new CaseSearchResult();
        caseSearchResult.setId(UUID.randomUUID());
        caseSearchResult.setCaseId(caseSummary.getId());
        caseSearchResult.setCaseSummary(caseSummary);
        caseSearchResult.setFirstName(firstName);
        caseSearchResult.setLastName(lastName);
        caseSearchResult.setCurrentFirstName(firstName);
        caseSearchResult.setCurrentLastName(lastName);
        return caseSearchResultRepository.save(caseSearchResult);
    }
}