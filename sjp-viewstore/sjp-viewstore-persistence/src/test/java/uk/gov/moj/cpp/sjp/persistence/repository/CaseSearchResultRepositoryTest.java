package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import uk.gov.justice.services.common.converter.LocalDates;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSearchResult;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseSummary;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseSearchResultRepositoryTest extends BaseTransactionalTest {

    private static final boolean IS_CURRENT = false;
    private static final boolean IS_OLD = true;
    private static final AtomicLong TIME = new AtomicLong(System.currentTimeMillis() - Duration.ofDays(1).toMillis());
    private static final String URN = "URN";
    private static final String LAST_NAME = "lastName";
    private static final String LAST_NAME_UPDATED = "updatedFamilyName";
    private static final String FIRST_NAME = "firstName";
    private static final String PROSECUTING_AUTHORITY_1 = "PROC1";
    private static final String PROSECUTING_AUTHORITY_2 = "PROC2";
    private static final String PROSECUTING_AUTHORITY_WILDCARD = "%";
    private static final String LEGAL_ENTITY_NAME = "legalEntityName";
    @Inject
    private CaseSearchResultRepository caseSearchResultRepository;
    @Inject
    private CaseSummaryRepository caseSummaryRepository;

    @Test
    public void shouldFindByLastName() {

        createCaseSearchResult(false);

        // check it is case insensitive
        final List<CaseSearchResult> results = caseSearchResultRepository.findByLastName(PROSECUTING_AUTHORITY_WILDCARD, LAST_NAME.toLowerCase());

        assertThat(results.get(0).getLastName(), equalTo(LAST_NAME));
        assertThat(results.get(0).getCaseSummary().getUrn(), equalTo(URN));
    }

    @Test
    public void shouldNotFindByLastNameForOtherProsecutingAuthority() {

        createCaseSearchResult(PROSECUTING_AUTHORITY_2);

        final List<CaseSearchResult> results = caseSearchResultRepository.findByLastName(PROSECUTING_AUTHORITY_1, LAST_NAME.toLowerCase());

        assertThat(results.size(), is(0));
    }

    @Test
    public void shouldFindByLastNameWithOrdering() {

        final CaseSearchResult caseSearchResultA = createCaseSearchResultWithFirstname("firstNameA", PROSECUTING_AUTHORITY_1);
        final CaseSearchResult caseSearchResultB = createCaseSearchResultWithFirstname("firstNameB", PROSECUTING_AUTHORITY_1);

        // check results come back ordered by first name ascending
        final List<CaseSearchResult> results = caseSearchResultRepository.findByLastName(PROSECUTING_AUTHORITY_WILDCARD, LAST_NAME.toLowerCase());

        assertThat(results.get(0).getLastName(), equalTo(LAST_NAME));
        assertThat(results.get(0).getFirstName(), equalTo(caseSearchResultA.getFirstName()));

        assertThat(results.get(1).getLastName(), equalTo(LAST_NAME));
        assertThat(results.get(1).getFirstName(), equalTo(caseSearchResultB.getFirstName()));
    }

    @Test
    public void shouldFindByLastNameEvenThoughItWasChanged() {
        // given
        final CaseSearchResult caseSearchResult = createCaseSearchResult(false);
        // when
        final List<CaseSearchResult> results = caseSearchResultRepository.findByLastName(PROSECUTING_AUTHORITY_1, LAST_NAME.toLowerCase());
        // then
        assertThat(results, hasSize(1));
        ensure(results.get(0), LAST_NAME, IS_CURRENT);

        // given
        simulateDefendantLastNameChanged(LAST_NAME_UPDATED, results.get(0).getCaseId());

        // when
        final List<CaseSearchResult> afterUpdateForOldLastName = caseSearchResultRepository.findByLastName(PROSECUTING_AUTHORITY_1, LAST_NAME.toLowerCase());
        // then
        assertThat(afterUpdateForOldLastName, hasSize(1));
        ensure(afterUpdateForOldLastName.get(0), LAST_NAME_UPDATED, IS_OLD);

        // when
        final List<CaseSearchResult> afterUpdateForNewLastName = caseSearchResultRepository.findByLastName(PROSECUTING_AUTHORITY_1, LAST_NAME_UPDATED.toLowerCase());
        // then
        assertThat(afterUpdateForNewLastName, hasSize(1));
        ensure(afterUpdateForNewLastName.get(0), LAST_NAME_UPDATED, IS_CURRENT);

        // given
        simulateDefendantLastNameChanged(LAST_NAME, caseSearchResult.getCaseId());

        // when
        final List<CaseSearchResult> afterUpdateBackForOriginalLastName = caseSearchResultRepository.findByLastName(PROSECUTING_AUTHORITY_1, LAST_NAME.toLowerCase());
        // then
        assertThat(afterUpdateBackForOriginalLastName, hasSize(1));
        ensure(afterUpdateBackForOriginalLastName.get(0), LAST_NAME, IS_CURRENT);

        // when
        final List<CaseSearchResult> afterUpdateBackForUpdatedButRevertedLastName = caseSearchResultRepository.findByLastName(PROSECUTING_AUTHORITY_1, LAST_NAME_UPDATED.toLowerCase());
        // then
        assertThat(afterUpdateBackForUpdatedButRevertedLastName, hasSize(1));
        ensure(afterUpdateBackForUpdatedButRevertedLastName.get(0), LAST_NAME, IS_OLD);

        // given last name was updated to LAST_NAME_UPDATED and a new case with LAST_NAME was created
        simulateDefendantLastNameChanged(LAST_NAME_UPDATED, afterUpdateBackForUpdatedButRevertedLastName.get(0).getCaseId());
        //simulateDefendantLastNameChanged(LAST_NAME_UPDATED, caseSearchResultRepository.findByUrn(PROSECUTING_AUTHORITY_1, URN));
        createCaseSearchResult(false);
        // when
        final List<CaseSearchResult> resultsAfterInsert = caseSearchResultRepository.findByLastName(PROSECUTING_AUTHORITY_1, LAST_NAME.toLowerCase());
        // then the results should contain 1 entry with LAST_NAME and 1 entry with LAST_NAME_UPDATED
        assertThat(resultsAfterInsert, hasSize(2));
        ensure(resultsAfterInsert.get(0), LAST_NAME_UPDATED, IS_OLD);
        ensure(resultsAfterInsert.get(1), LAST_NAME, IS_CURRENT);
    }

    private void ensure(final CaseSearchResult caseSearchResult, final String lastName, final boolean deprecated) {
        assertThat(caseSearchResult.getCurrentLastName(), equalTo(lastName));
        assertThat(caseSearchResult.isDeprecated(), is(deprecated));
    }

    @Test
    public void shouldFindByUrn() {

        final CaseSearchResult caseSearchResult = createCaseSearchResult(false);

        // check it is case insensitive
        final List<CaseSearchResult> results = caseSearchResultRepository.findByUrn(PROSECUTING_AUTHORITY_WILDCARD, URN.toLowerCase());

        assertThat(results.get(0).getLastName(), equalTo(caseSearchResult.getLastName()));
        assertThat(results.get(0).getCaseSummary().getUrn(), equalTo(caseSearchResult.getCaseSummary().getUrn()));
    }

    @Test
    public void shouldNotFindByUrnForOtherProsecutingAuthority() {

        createCaseSearchResult(PROSECUTING_AUTHORITY_2);

        // check it is case insensitive
        final List<CaseSearchResult> results = caseSearchResultRepository.findByUrn(PROSECUTING_AUTHORITY_1, URN.toLowerCase());

        assertThat(results.size(), is(0));
    }

    @Test
    public void shouldFindByUrnOnlyLatest() {
        // given
        final CaseSearchResult caseSearchResult = createCaseSearchResult(false);
        simulateDefendantLastNameChanged(LAST_NAME_UPDATED, caseSearchResult.getCaseId());
        simulateDefendantLastNameChanged(LAST_NAME, caseSearchResult.getCaseId());

        // when
        final List<CaseSearchResult> resultsByUrn = caseSearchResultRepository.findByUrn(PROSECUTING_AUTHORITY_1, URN);
        final List<CaseSearchResult> resultsByCaseId = caseSearchResultRepository.findByCaseId(caseSearchResult.getCaseId());

        // then
        assertThat(resultsByUrn, hasSize(1));
        assertThat(resultsByCaseId, hasSize(3));

        ensure(resultsByUrn.get(0), LAST_NAME, IS_CURRENT);
    }

    @Test
    public void shouldFindByCaseId() {

        final CaseSearchResult caseSearchResult = createCaseSearchResult(false);
        final UUID caseId = caseSearchResult.getCaseId();

        final List<CaseSearchResult> results = caseSearchResultRepository.findByCaseId(caseId);

        assertThat(results.get(0).getLastName(), equalTo(LAST_NAME));
        assertThat(results.get(0).getCaseSummary().getUrn(), equalTo(URN));
        assertThat(results.get(0).getCaseSummary().getId(), equalTo(caseId));

        assertThat(results.size(), is(1));
    }

    @Test
    public void shouldCreateWithoutCaseSummary() {
        final CaseSearchResult caseSearchResult = new CaseSearchResult(UUID.randomUUID(), UUID.randomUUID(), "firstName", "lastName", LocalDates.from("2001-02-03"), null, null);

        caseSearchResultRepository.save(caseSearchResult);

        assertThat(caseSearchResultRepository.findBy(caseSearchResult.getId()), notNullValue());
    }

    private CaseSearchResult createCaseSearchResult(final boolean defendantIsCompany) {
        if (defendantIsCompany) {
            return createCaseSearchResultWithLegalEntity(UUID.randomUUID(), URN, PROSECUTING_AUTHORITY_1, LEGAL_ENTITY_NAME);
        } else {
            return createCaseSearchResultWithFirstname(FIRST_NAME, PROSECUTING_AUTHORITY_1);
        }
    }

    private CaseSearchResult createCaseSearchResult(final String prosecutingAuthority) {
        return createCaseSearchResultWithFirstname(FIRST_NAME, prosecutingAuthority);
    }

    private CaseSearchResult createCaseSearchResultWithFirstname(final String firstName, final String prosecutingAuthority) {
        return createCaseSearchResultWith(UUID.randomUUID(), URN, firstName, LAST_NAME, prosecutingAuthority, LEGAL_ENTITY_NAME);
    }

    private CaseSearchResult createCaseSearchResultWithLegalEntityName(final String legalEntityName, final String prosecutingAuthority) {
        return createCaseSearchResultWith(UUID.randomUUID(), URN, FIRST_NAME, LAST_NAME, prosecutingAuthority, legalEntityName);
    }

    private void simulateDefendantLastNameChanged(final String newLastName, final UUID caseId) {
        final List<CaseSearchResult> caseSearchResults = caseSearchResultRepository.findByCaseId(caseId);
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

    private CaseSearchResult createCaseSearchResultWith(final UUID caseId, final String urn, final String firstName, final String lastName) {
        return createCaseSearchResultWith(caseId, urn, firstName, lastName, PROSECUTING_AUTHORITY_1, LEGAL_ENTITY_NAME);
    }

    private CaseSearchResult createCaseSearchResultWith(
            final UUID caseId, final String urn, final String firstName, final String lastName, final String prosecutingAuthority, final String legalEntityName) {
        CaseSummary caseSummary = new CaseSummary();
        caseSummary.setId(caseId);
        caseSummary.setUrn(urn);
        caseSummary.setProsecutingAuthority(prosecutingAuthority);
        caseSummary = caseSummaryRepository.save(caseSummary);

        final CaseSearchResult caseSearchResult = new CaseSearchResult();
        caseSearchResult.setId(UUID.randomUUID());
        caseSearchResult.setCaseId(caseSummary.getId());
        caseSearchResult.setCaseSummary(caseSummary);
        caseSearchResult.setFirstName(firstName);
        caseSearchResult.setLastName(lastName);
        caseSearchResult.setDefendantId(UUID.randomUUID());
        caseSearchResult.setCurrentFirstName(firstName);
        caseSearchResult.setCurrentLastName(lastName);
        caseSearchResult.setLegalEntityName(legalEntityName);
        caseSearchResult.setDateAdded(ZonedDateTime.ofInstant(Instant.ofEpochMilli(TIME.getAndAdd(100)), ZoneId.systemDefault()));
        return caseSearchResultRepository.save(caseSearchResult);
    }

    private CaseSearchResult createCaseSearchResultWithLegalEntity(
            final UUID caseId, final String urn, final String prosecutingAuthority, final String legalEntityName) {
        CaseSummary caseSummary = new CaseSummary();
        caseSummary.setId(caseId);
        caseSummary.setUrn(urn);
        caseSummary.setProsecutingAuthority(prosecutingAuthority);
        caseSummary = caseSummaryRepository.save(caseSummary);

        final CaseSearchResult caseSearchResult = new CaseSearchResult();
        caseSearchResult.setId(UUID.randomUUID());
        caseSearchResult.setCaseId(caseSummary.getId());
        caseSearchResult.setCaseSummary(caseSummary);
        caseSearchResult.setDefendantId(UUID.randomUUID());
        caseSearchResult.setLegalEntityName(legalEntityName);
        caseSearchResult.setDateAdded(ZonedDateTime.ofInstant(Instant.ofEpochMilli(TIME.getAndAdd(100)), ZoneId.systemDefault()));
        return caseSearchResultRepository.save(caseSearchResult);
    }

    @Test
    public void shouldFindByLegalEntityName() {
        createCaseSearchResult(true);
        // check it is case insensitive
        final List<CaseSearchResult> results = caseSearchResultRepository.findByLegalEntityName(PROSECUTING_AUTHORITY_WILDCARD, LEGAL_ENTITY_NAME.toLowerCase());

        assertEquals(LEGAL_ENTITY_NAME, results.get(0).getLegalEntityName());
        assertEquals(URN, results.get(0).getCaseSummary().getUrn());
    }
}