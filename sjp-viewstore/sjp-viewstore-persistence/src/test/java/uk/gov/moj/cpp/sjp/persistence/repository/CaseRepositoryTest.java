package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.time.ZoneOffset.UTC;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseRepositoryTest extends BaseTransactionalTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<UUID, CaseDetail> CASE_HOLDER = new HashMap<>();

    private static final UUID VALID_CASE_ID_1 = randomUUID();
    private static final UUID VALID_CASE_ID_2 = randomUUID();
    private static final UUID VALID_CASE_ID_3 = randomUUID();
    private static final UUID VALID_CASE_ID_4 = randomUUID();

    private static final UUID VALID_DEFENDANT_ID_1 = randomUUID();
    private static final UUID VALID_DEFENDANT_ID_2 = randomUUID();
    private static final UUID VALID_DEFENDANT_ID_4 = randomUUID();
    private static final UUID VALID_MATERIAL_ID = randomUUID();

    private static final int NUM_PREVIOUS_CONVICTIONS = 3;
    private static final BigDecimal COSTS = BigDecimal.valueOf(10.33);
    private static final LocalDate POSTING_DATE = LocalDate.parse("2015-12-02", FORMATTER);
    private static final String ENTERPRISE_ID = "2K2SLYFC743H";

    @Inject
    private CaseRepository caseRepository;

    private ZonedDateTime caseCreatedOn = ZonedDateTime.now(UTC);

    @Override
    public void setUpBefore() {
        // given 3 cases exist in database
        CaseDetail case1 = getCase(VALID_CASE_ID_1, VALID_DEFENDANT_ID_1);
        case1.setEnterpriseId(ENTERPRISE_ID);
        // case 2 is withdrawn
        CaseDetail case2 = getCase(VALID_CASE_ID_2, VALID_DEFENDANT_ID_2, VALID_MATERIAL_ID, true);
        CaseDetail case3 = getCase(VALID_CASE_ID_3);
        CaseDetail case4 = getCase(VALID_CASE_ID_4, VALID_DEFENDANT_ID_4);

        caseRepository.save(case1);
        caseRepository.save(case2);
        caseRepository.save(case3);
        caseRepository.save(case4);

        caseRepository.save(case2);

        caseRepository.save(case3);
        caseRepository.save(case3);

        // putting it in a holder for easy retrieval and subsequent deletion during teardown
        CASE_HOLDER.put(VALID_CASE_ID_1, case1);
        CASE_HOLDER.put(VALID_CASE_ID_2, case2);
        CASE_HOLDER.put(VALID_CASE_ID_3, case3);
        CASE_HOLDER.put(VALID_CASE_ID_4, case4);
    }

    //    @Override
    @After
    public void tearDownAfterTemporary() {
        // cleaning up database after each test to avoid data collision
        CASE_HOLDER.values().forEach(caseRepository::attachAndRemove);
    }

    @Test
    public void shouldFindCaseMatchingUrn() {
        CaseDetail actualCase = caseRepository.findByUrn(getUrnForCaseId(VALID_CASE_ID_1));
        assertNotNull(actualCase);
        assertEquals("ID should match ID of case 1", VALID_CASE_ID_1, actualCase.getId());
        assertThat(caseCreatedOn, is(actualCase.getDateTimeCreated()));

    }

    @Test
    public void shouldFindCaseMatchingUrnIgnoringCase() {
        CaseDetail actualCase = caseRepository.findByUrn(getUrnForCaseId(VALID_CASE_ID_1).toLowerCase());
        assertNotNull(actualCase);
        assertEquals("ID should match ID of case 1", VALID_CASE_ID_1, actualCase.getId());
    }

    @Test
    public void shouldFindCaseByPersonId() {
        List<CaseDetail> caseDetails = caseRepository.findByDefendantId(VALID_DEFENDANT_ID_1);
        assertNotNull(caseDetails);
        assertThat("Should have 1 entry", caseDetails, hasSize(1));
        assertEquals("ID should match ID of case 1", VALID_CASE_ID_1, caseDetails.get(0).getId());
    }

    @Test
    public void shouldFindCaseWithEnterpriseIdByCaseId() {
        final CaseDetail caseDetail = caseRepository.findBy(VALID_CASE_ID_1);

        assertThat(caseDetail.getEnterpriseId(), equalTo(ENTERPRISE_ID));
    }

    @Test
    public void shouldFindCaseDocuments() {
        List<CaseDocument> caseDocuments = caseRepository.findCaseDocuments(VALID_CASE_ID_2);
        assertNotNull(caseDocuments);
        assertThat("Should have 1 entry", caseDocuments, hasSize(1));
        assertEquals("ID should match valid material ID", VALID_MATERIAL_ID,
                caseDocuments.get(0).getMaterialId());
    }

    @Test
    public void shouldFindCaseDefendants_Success() {
        DefendantDetail defendant = caseRepository.findCaseDefendant(VALID_CASE_ID_3);
        assertNotNull(defendant);
        assertEquals(VALID_CASE_ID_3, defendant.getCaseDetail().getId());
    }

    @Test
    public void shouldCompleteCaseSuccessfully() {
        assertNull("CaseAggregate should not be completed",
                CASE_HOLDER.get(VALID_CASE_ID_1).getCompleted());

        caseRepository.completeCase(VALID_CASE_ID_1);
        CaseDetail actualCase = caseRepository.findBy(VALID_CASE_ID_1);
        assertTrue("CaseAggregate should be completed", actualCase.getCompleted());
    }

    @Test
    public void shouldUpdateLibraCaseReopenedDetails() {
        LocalDate reopenedDate = LocalDate.now();
        final String reason = "REASON";
        CaseDetail actualCase = caseRepository.findByUrn(getUrnForCaseId(VALID_CASE_ID_1));
        actualCase.setLibraCaseNumber("LIBRA12345");
        actualCase.setReopenedDate(reopenedDate);
        actualCase.setReopenedInLibraReason(reason);
        caseRepository.save(actualCase);
        CaseDetail expectedCaseDetails = caseRepository.findBy(VALID_CASE_ID_1);
        assertEquals("LIBRA12345", expectedCaseDetails.getLibraCaseNumber());
        assertEquals(reopenedDate, expectedCaseDetails.getReopenedDate());
        assertEquals(reason, expectedCaseDetails.getReopenedInLibraReason());
    }

    @Test
    public void shouldPersistWithdrawnInformation() {
        //given case_2 is withdrawn

        CaseDetail caseDetailForWithdrawnCase = caseRepository.findBy(VALID_CASE_ID_2);

        // all offence are withdrawn
        isCasePendingWithdrawal(caseDetailForWithdrawnCase);

        CaseDetail caseDetailForNotWithdrawnCase = caseRepository.findBy(VALID_CASE_ID_1);
        isCaseNotPendingWithdrawal(caseDetailForNotWithdrawnCase);

        caseRepository.requestWithdrawalAllOffences(VALID_CASE_ID_1);

        caseDetailForWithdrawnCase = caseRepository.findBy(VALID_CASE_ID_1);
        isCasePendingWithdrawal(caseDetailForWithdrawnCase);
    }

    @Test
    public void shouldPersistCancelWithdrawnInformation() {
        //given: case_2 is withdrawn
        isCasePendingWithdrawal(caseRepository.findBy(VALID_CASE_ID_2));

        //when: cancel withdraw request
        caseRepository.cancelRequestWithdrawalAllOffences(VALID_CASE_ID_2);

        //then: no longer withdrawn
        isCaseNotPendingWithdrawal(caseRepository.findBy(VALID_CASE_ID_2));
    }

    @Test
    public void shouldFindCaseByMaterialIdWhenMaterialIsDocument() {

        CaseDetail actualCase = caseRepository.findByUrn(getUrnForCaseId(VALID_CASE_ID_2));

        CaseDetail caseReturned = caseRepository.findByMaterialId(VALID_MATERIAL_ID);

        assertThat(caseReturned.getId(), is(actualCase.getId()));
    }


    @Test
    public void shouldPersistCurrencyAndOtherSupportingInformation() {
        CaseDetail caseDetail = caseRepository.findBy(VALID_CASE_ID_1);

        assertThat(caseDetail.getDefendant().getNumPreviousConvictions(), is(NUM_PREVIOUS_CONVICTIONS));
        assertThat(caseDetail.getCosts(), is(COSTS));
        assertThat(caseDetail.getPostingDate(), is(POSTING_DATE));

        DefendantDetail defendantDetail = caseDetail.getDefendant();
        OffenceDetail offenceDetail = defendantDetail.getOffences().iterator().next();

        assertThat(offenceDetail.getWitnessStatement(), is("witness statement"));
        assertThat(offenceDetail.getProsecutionFacts(), is("prosecution facts"));
        assertThat(offenceDetail.getLibraOffenceDateCode(), is(30));

    }

    @Test
    public void shouldNotFindNonExistingCase() {
        final UUID unkownId = UUID.randomUUID();
        final CaseDetail caseDetail = caseRepository.findBy(unkownId);

        assertThat(caseDetail, nullValue());
    }

    @Test
    public void shouldFindOldestUncompletedPostingDate() {

        final LocalDate oldestPostingDate = POSTING_DATE.minusDays(1);

        final CaseDetail oldestUncompletedCase = getCase(randomUUID());
        oldestUncompletedCase.setCompleted(false);
        oldestUncompletedCase.setPostingDate(oldestPostingDate);
        caseRepository.save(oldestUncompletedCase);

        final CaseDetail completedCase = getCase(randomUUID());
        completedCase.setCompleted(true);
        completedCase.setPostingDate(POSTING_DATE.minusDays(2));
        caseRepository.save(completedCase);

        // for deletion after the test has run
        CASE_HOLDER.put(completedCase.getId(), completedCase);
        CASE_HOLDER.put(oldestUncompletedCase.getId(), oldestUncompletedCase);

        assertThat(caseRepository.findOldestUncompletedPostingDate(), equalTo(oldestPostingDate));
    }

    private void isCaseNotPendingWithdrawal(CaseDetail caseDetail) {
        checkAllOffencesForACase(caseDetail, false);
    }

    private void checkAllOffencesForACase(CaseDetail caseDetail, boolean withdrawn) {
        Set<OffenceDetail> offences = caseDetail.getDefendant().getOffences();

        assertThat(offences, not(empty()));
        offences.forEach(offence -> assertEquals(withdrawn, offence.getPendingWithdrawal()));
    }

    private void isCasePendingWithdrawal(CaseDetail caseDetail) {
        checkAllOffencesForACase(caseDetail, true);
    }

    private CaseDetail getCase(UUID caseId) {
        return getCase(caseId, randomUUID());
    }

    private CaseDetail getCase(UUID caseId, UUID defendantId) {
        return getCase(caseId, defendantId, randomUUID(), false);
    }

    private CaseDetail getCase(UUID caseId, UUID defendantId, UUID materialId, boolean withdrawn) {


        final DefendantDetail defendantDetail = DefendantDetailBuilder.aDefendantDetail()
                .withId(defendantId)
                .withOffencePendingWithdrawal(withdrawn)
                .build();

        final CaseDetail caseDetail = CaseDetailBuilder.aCase()
                .withCaseId(caseId)
                .withUrn(getUrnForCaseId(caseId))
                .withCosts(COSTS)
                .withPostingDate(POSTING_DATE)
                .addDefendantDetail(defendantDetail)
                .addCaseDocument(getCaseDocument(caseId, materialId))
                .withCreatedOn(caseCreatedOn)
                .build();
        // assuming there is just one defendant for now
        caseDetail.getDefendant().setNumPreviousConvictions(NUM_PREVIOUS_CONVICTIONS);
        return caseDetail;
    }


    private CaseDocument getCaseDocument(UUID caseId, UUID materialId) {
        return new CaseDocument(randomUUID(), materialId, "SJPN", ZonedDateTime.now(), caseId, 1);
    }

    private String getUrnForCaseId(final UUID caseId) {
        return "TFL" + caseId.toString();
    }

}
