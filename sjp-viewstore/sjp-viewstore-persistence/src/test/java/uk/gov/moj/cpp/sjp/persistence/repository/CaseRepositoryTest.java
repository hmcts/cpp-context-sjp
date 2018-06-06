package uk.gov.moj.cpp.sjp.persistence.repository;

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
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
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
import javax.persistence.NonUniqueResultException;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseRepositoryTest extends BaseTransactionalTest {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final Map<UUID, CaseDetail> CASE_HOLDER = new HashMap<>();

    private static final ProsecutingAuthority PROSECUTING_AUTHORITY = TFL;
    private static final String PROSECUTING_AUTHORITY_PREFIX = PROSECUTING_AUTHORITY.name();

    private static final UUID VALID_CASE_ID_1 = randomUUID();
    private static final UUID VALID_CASE_ID_2 = randomUUID();
    private static final UUID VALID_CASE_ID_3 = randomUUID();
    private static final UUID VALID_CASE_ID_4 = randomUUID();

    private static final String VALID_URN_1 = randomUrn();
    private static final String VALID_URN_2 = randomUrn();
    private static final String VALID_URN_3 = randomUrn();
    private static final String VALID_URN_4 = randomUrn();

    private static final UUID VALID_DEFENDANT_ID_1 = randomUUID();
    private static final UUID VALID_DEFENDANT_ID_2 = randomUUID();
    private static final UUID VALID_DEFENDANT_ID_4 = randomUUID();
    private static final UUID VALID_MATERIAL_ID = randomUUID();

    private static final int NUM_PREVIOUS_CONVICTIONS = 3;
    private static final BigDecimal COSTS = BigDecimal.valueOf(10.33);
    private static final LocalDate POSTING_DATE = LocalDate.parse("2015-12-02", FORMATTER);
    private static final String ENTERPRISE_ID = "2K2SLYFC743H";
    private static final String POSTCODE = "CR0 1AB";

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private Clock clock;

    private ZonedDateTime caseCreatedOn;

    @Override
    public void setUpBefore() {
        caseCreatedOn = clock.now();
        // given 3 cases exist in database
        CaseDetail case1 = getCase(VALID_CASE_ID_1, VALID_URN_1, VALID_DEFENDANT_ID_1);
        case1.setInitiationCode("J");
        case1.setEnterpriseId(ENTERPRISE_ID);
        // case 2 is withdrawn
        CaseDetail case2 = getCase(VALID_CASE_ID_2, VALID_URN_2, VALID_DEFENDANT_ID_2, VALID_MATERIAL_ID, true, POSTCODE);
        CaseDetail case3 = getCase(VALID_CASE_ID_3, VALID_URN_3);
        CaseDetail case4 = getCase(VALID_CASE_ID_4, VALID_URN_4, VALID_DEFENDANT_ID_4);

        CaseDocument caseDocument = case2.getCaseDocuments().iterator().next();

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
        CaseDetail actualCase = caseRepository.findByUrn(VALID_URN_1);
        assertNotNull(actualCase);
        assertEquals("ID should match ID of case 1", VALID_CASE_ID_1, actualCase.getId());
        assertThat(caseCreatedOn, is(actualCase.getDateTimeCreated()));

    }

    @Test
    public void shouldFindCaseMatchingUrnIgnoringCase() {
        CaseDetail actualCase = caseRepository.findByUrn(VALID_URN_1.toLowerCase());
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
        CaseDetail actualCase = caseRepository.findByUrn(VALID_URN_1);
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

        CaseDetail actualCase = caseRepository.findByUrn(VALID_URN_2);

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
        final CaseDetail caseDetail = caseRepository.findBy(UUID.randomUUID());

        assertThat(caseDetail, nullValue());
    }

    @Test
    public void shouldFindOldestUncompletedPostingDate() {

        final LocalDate oldestPostingDate = POSTING_DATE.minusDays(1);

        final CaseDetail oldestUncompletedCase = getCase(randomUUID(), randomUrn());
        oldestUncompletedCase.setInitiationCode("J");
        oldestUncompletedCase.setCompleted(false);
        oldestUncompletedCase.setPostingDate(oldestPostingDate);
        caseRepository.save(oldestUncompletedCase);

        final CaseDetail completedCase = getCase(randomUUID(), randomUrn());
        completedCase.setInitiationCode("J");
        completedCase.setCompleted(true);
        completedCase.setPostingDate(POSTING_DATE.minusDays(2));
        caseRepository.save(completedCase);

        // for deletion after the test has run
        CASE_HOLDER.put(completedCase.getId(), completedCase);
        CASE_HOLDER.put(oldestUncompletedCase.getId(), oldestUncompletedCase);

        assertThat(caseRepository.findOldestUncompletedPostingDate(), equalTo(oldestPostingDate));
    }

    @Test
    public void shouldFindCaseMatchingUrnWithPrefixAndPostcode() {
        final CaseDetail actualCase = caseRepository.findByUrnPostcode(VALID_URN_1, POSTCODE);

        assertNotNull(actualCase);
        assertEquals("ID should match ID of case 1", VALID_CASE_ID_1, actualCase.getId());
        assertEquals("URN should match URN of case 1", VALID_URN_1, actualCase.getUrn());

    }

    @Test
    public void shouldFindCaseMatchingUrnWithPrefixAndPostcodeWithExtraSpaces() {
        final CaseDetail actualCase = caseRepository.findByUrnPostcode(VALID_URN_1, String.format("  %s   ", POSTCODE));

        assertNotNull(actualCase);
        assertEquals("ID should match ID of case 1", VALID_CASE_ID_1, actualCase.getId());
        assertEquals("URN should match URN of case 1", VALID_URN_1, actualCase.getUrn());

    }

    @Test
    public void shouldFindCaseMatchingUrnWithoutPrefixAndPostcode() {
        final CaseDetail actualCase = caseRepository.findByUrnPostcode(VALID_URN_1.replace(PROSECUTING_AUTHORITY_PREFIX, ""), POSTCODE);

        assertNotNull(actualCase);
        assertEquals("ID should match ID of case 1", VALID_CASE_ID_1, actualCase.getId());
        assertEquals("URN should match URN of case 1", VALID_URN_1, actualCase.getUrn());

    }

    @Test
    public void shouldFindCaseWhenUrnWithoutPrefixSameButPostcodeDifferent() {

        //given
        final String urn1 = "TFL12345678A";
        final String postcode1 = "AB1 2CD";

        final String urn2 = "TVL12345678A";
        final String postcode2 = "EF1 2GH";

        final CaseDetail caseDetail1 = getCase(randomUUID(), urn1, postcode1);
        final CaseDetail caseDetail2 = getCase(randomUUID(), urn2, postcode2);

        caseRepository.save(caseDetail1);
        caseRepository.save(caseDetail2);
        CASE_HOLDER.put(caseDetail1.getId(), caseDetail1);
        CASE_HOLDER.put(caseDetail2.getId(), caseDetail2);

        //when
        final CaseDetail actualCase = caseRepository.findByUrnPostcode("12345678A", postcode1);

        //then
        assertEquals(urn1, actualCase.getUrn());
    }

    @Test(expected = NonUniqueResultException.class)
    public void shouldThrowExceptionWhenTwoCasesHaveSameUrnWithoutPrefixAndPostcode() {

        //given
        final String urn1 = "TFL12345678A";
        final String postcode1 = "AB1 2CD";

        final String urn2 = "TVL12345678A";
        final String postcode2 = "AB1 2CD";

        final CaseDetail caseDetail1 = getCase(randomUUID(), urn1, postcode1);
        final CaseDetail caseDetail2 = getCase(randomUUID(), urn2, postcode2);

        caseRepository.save(caseDetail1);
        caseRepository.save(caseDetail2);
        CASE_HOLDER.put(caseDetail1.getId(), caseDetail1);
        CASE_HOLDER.put(caseDetail2.getId(), caseDetail2);

        //when
        final CaseDetail actualCase = caseRepository.findByUrnPostcode("12345678A", postcode1);

        //then throws exception
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

    private CaseDetail getCase(UUID caseId, String urn) {
        return getCase(caseId, urn, randomUUID());
    }

    private CaseDetail getCase(UUID caseId, String urn, String postcode) {
        return getCase(caseId, urn, randomUUID(), randomUUID(), false, postcode);
    }

    private CaseDetail getCase(UUID caseId, String urn, UUID defendantId) {
        return getCase(caseId, urn, defendantId, randomUUID(), false, POSTCODE);
    }

    private CaseDetail getCase(UUID caseId, String urn, UUID defendantId, UUID materialId, boolean withdrawn, String postcode) {

        final DefendantDetail defendantDetail = DefendantDetailBuilder.aDefendantDetail()
                .withId(defendantId)
                .withOffencePendingWithdrawal(withdrawn)
                .withPostcode(postcode)
                .build();


        final CaseDetail caseDetail = CaseDetailBuilder.aCase()
                .withCaseId(caseId)
                .withUrn(urn)
                .withProsecutingAuthority(PROSECUTING_AUTHORITY)
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
        return new CaseDocument(randomUUID(), materialId, "SJPN", clock.now(), caseId, 1);
    }

    private static String randomUrn() {
        return PROSECUTING_AUTHORITY_PREFIX + RandomGenerator.integer(100000000, 999999999).next();
    }

}
