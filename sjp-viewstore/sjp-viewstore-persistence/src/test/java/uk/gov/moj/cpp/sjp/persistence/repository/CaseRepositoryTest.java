package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NonUniqueResultException;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseRepositoryTest extends BaseTransactionalTest {

    private static final Map<UUID, CaseDetail> CASES = new HashMap<>();

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
    private static final UUID VALID_MATERIAL_ID_1 = randomUUID();
    private static final UUID VALID_MATERIAL_ID_2 = randomUUID();

    private static final int NUM_PREVIOUS_CONVICTIONS = 3;
    private static final BigDecimal COSTS = BigDecimal.valueOf(10.33);
    private static final String ENTERPRISE_ID = "2K2SLYFC743H";
    private static final String POSTCODE = "CR0 1AB";
    private static final String OFFENCE_CODE = "PS0001";
    private static LocalDate postingDate = LocalDate.of(2015, 12, 31);

    private static final List<ReadyCase> READY_CASES = new ArrayList<>();

    @Inject
    private EntityManager entityManager;

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private ReadyCaseRepository readyCaseRepository;

    @Inject
    private Clock clock;

    private ZonedDateTime caseCreatedOn;

    private CaseDetail case1, case2, case3, case4;

    @Override
    public void setUpBefore() {
        caseCreatedOn = clock.now();
        // given 4 cases exist in database
        case1 = getCase(VALID_CASE_ID_1, VALID_URN_1, VALID_DEFENDANT_ID_1);
        case1.setEnterpriseId(ENTERPRISE_ID);
        // case 2 is withdrawn
        case2 = getCase(VALID_CASE_ID_2, VALID_URN_2, VALID_DEFENDANT_ID_2, true, POSTCODE, VALID_MATERIAL_ID_1, VALID_MATERIAL_ID_2);
        case3 = getCase(VALID_CASE_ID_3, VALID_URN_3);
        case4 = getCase(VALID_CASE_ID_4, VALID_URN_4, VALID_DEFENDANT_ID_4);

        CASES.put(VALID_CASE_ID_1, case1);
        CASES.put(VALID_CASE_ID_2, case2);
        CASES.put(VALID_CASE_ID_3, case3);
        CASES.put(VALID_CASE_ID_4, case4);

        CASES.values().forEach(caseRepository::save);

        // test duplicate cases aren't possible
        caseRepository.save(case1);

        READY_CASES.add(new ReadyCase(case1.getId(), CaseReadinessReason.PIA));
        READY_CASES.add(new ReadyCase(case2.getId(), CaseReadinessReason.WITHDRAWAL_REQUESTED));
        READY_CASES.add(new ReadyCase(case3.getId(), CaseReadinessReason.PLEADED_GUILTY));
        // leave case 4 as not ready

        READY_CASES.forEach(readyCaseRepository::save);

        //clear L1 cache to force JPA to execute query against database
        entityManager.clear();
    }

    @After
    public void tearDownAfterTemporary() {
        // cleaning up database after each test to avoid data collision
        CASES.values().forEach(caseRepository::attachAndRemove);
        READY_CASES.forEach(readyCaseRepository::attachAndRemove);
    }

    @Test
    public void shouldFindCase() {
        final CaseDetail actualCase = caseRepository.findBy(VALID_CASE_ID_1);
        assertNotNull(actualCase);
    }

    @Test
    public void shouldFindCaseWithMultipleDocuments() {
        final CaseDetail actualCase = caseRepository.findBy(VALID_CASE_ID_2);
        assertNotNull(actualCase);
        assertThat(actualCase.getCaseDocuments(), hasSize(case2.getCaseDocuments().size()));
        assertThat(actualCase.getCaseSearchResults(), hasSize(case2.getCaseSearchResults().size()));
        assertThat(actualCase.getDefendant().getOffences(), hasSize(case2.getDefendant().getOffences().size()));
    }

    @Test
    public void shouldFindCaseMatchingUrn() {
        final CaseDetail actualCase = caseRepository.findByUrn(VALID_URN_1);
        assertNotNull(actualCase);
        assertEquals("ID should match ID of case 1", VALID_CASE_ID_1, actualCase.getId());
        assertThat(caseCreatedOn, is(actualCase.getDateTimeCreated()));
    }

    @Test
    public void shouldFindCaseMatchingUrnIgnoringCase() {
        final CaseDetail actualCase = caseRepository.findByUrn(VALID_URN_1.toLowerCase());
        assertNotNull(actualCase);
        assertEquals("ID should match ID of case 1", VALID_CASE_ID_1, actualCase.getId());
    }

    @Test
    public void shouldFindCaseByPersonId() {
        final List<CaseDetail> caseDetails = caseRepository.findByDefendantId(VALID_DEFENDANT_ID_1);
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
        final List<CaseDocument> caseDocuments = caseRepository.findCaseDocuments(VALID_CASE_ID_2);
        assertNotNull(caseDocuments);
        assertThat(caseDocuments.stream().map(CaseDocument::getMaterialId).collect(toList()), containsInAnyOrder(VALID_MATERIAL_ID_1, VALID_MATERIAL_ID_2));
    }

    @Test
    public void shouldFindCaseDefendants_Success() {
        final DefendantDetail defendant = caseRepository.findCaseDefendant(VALID_CASE_ID_3);
        assertNotNull(defendant);
        assertEquals(VALID_CASE_ID_3, defendant.getCaseDetail().getId());
    }

    @Test
    public void shouldCompleteCaseSuccessfully() {
        assertFalse("CaseAggregate should not be completed",
                CASES.get(VALID_CASE_ID_1).isCompleted());

        caseRepository.completeCase(VALID_CASE_ID_1);
        CaseDetail actualCase = caseRepository.findBy(VALID_CASE_ID_1);
        assertTrue("CaseAggregate should be completed", actualCase.isCompleted());
        assertNull(actualCase.getAdjournedTo());
    }

    @Test
    public void shouldUpdateLibraCaseReopenedDetails() {
        final LocalDate reopenedDate = LocalDate.now();
        final String reason = "REASON";
        final CaseDetail actualCase = caseRepository.findByUrn(VALID_URN_1);
        actualCase.setLibraCaseNumber("LIBRA12345");
        actualCase.setReopenedDate(reopenedDate);
        actualCase.setReopenedInLibraReason(reason);
        caseRepository.save(actualCase);
        final CaseDetail expectedCaseDetails = caseRepository.findBy(VALID_CASE_ID_1);
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

        final CaseDetail caseDetailForNotWithdrawnCase = caseRepository.findBy(VALID_CASE_ID_1);
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

        final CaseDetail actualCase = caseRepository.findByUrn(VALID_URN_2);

        final CaseDetail caseReturned = caseRepository.findByMaterialId(VALID_MATERIAL_ID_1);

        assertThat(caseReturned.getId(), is(actualCase.getId()));
    }


    @Test
    public void shouldPersistCurrencyAndOtherSupportingInformation() {
        final CaseDetail caseDetail = caseRepository.findBy(VALID_CASE_ID_1);

        assertThat(caseDetail.getDefendant().getNumPreviousConvictions(), is(NUM_PREVIOUS_CONVICTIONS));
        assertThat(caseDetail.getCosts(), is(COSTS));
        assertThat(caseDetail.getPostingDate(), is(CASES.get(VALID_CASE_ID_1).getPostingDate()));

        final DefendantDetail defendantDetail = caseDetail.getDefendant();
        final OffenceDetail offenceDetail = defendantDetail.getOffences().iterator().next();

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
        CASES.put(caseDetail1.getId(), caseDetail1);
        CASES.put(caseDetail2.getId(), caseDetail2);

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
        CASES.put(caseDetail1.getId(), caseDetail1);
        CASES.put(caseDetail2.getId(), caseDetail2);

        //when
        final CaseDetail actualCase = caseRepository.findByUrnPostcode("12345678A", postcode1);

        //then throws exception
    }

    private void isCaseNotPendingWithdrawal(CaseDetail caseDetail) {
        checkAllOffencesForACase(caseDetail, false);
        assertFalse(caseDetail.isAnyOffencePendingWithdrawal());
    }

    private void checkAllOffencesForACase(CaseDetail caseDetail, boolean withdrawn) {
        final List<OffenceDetail> offences = caseDetail.getDefendant().getOffences();

        assertThat(offences, not(empty()));
        offences.forEach(offence -> assertEquals(withdrawn, offence.getPendingWithdrawal()));
    }

    private void isCasePendingWithdrawal(CaseDetail caseDetail) {
        checkAllOffencesForACase(caseDetail, true);
        assertTrue(caseDetail.isAnyOffencePendingWithdrawal());
    }

    private CaseDetail getCase(UUID caseId, String urn) {
        return getCase(caseId, urn, randomUUID());
    }

    private CaseDetail getCase(UUID caseId, String urn, String postcode) {
        return getCase(caseId, urn, randomUUID(), false, postcode, randomUUID());
    }

    private CaseDetail getCase(UUID caseId, String urn, UUID defendantId) {
        return getCase(caseId, urn, defendantId, false, POSTCODE, randomUUID());
    }

    private CaseDetail getCase(UUID caseId, String urn, UUID defendantId, boolean withdrawn, String postcode, UUID... materialIds) {

        final DefendantDetail defendantDetail = DefendantDetailBuilder.aDefendantDetail()
                .withId(defendantId)
                .withOffencePendingWithdrawal(withdrawn)
                .withPostcode(postcode)
                .withLastName(RandomGenerator.string(10).next())
                .withOffenceCode(OFFENCE_CODE)
                .withNumberOfPreviousConvictions(NUM_PREVIOUS_CONVICTIONS)
                .build();

        postingDate = postingDate.minusDays(1);

        final CaseDetailBuilder caseDetailBuilder = CaseDetailBuilder.aCase()
                .withCaseId(caseId)
                .withUrn(urn)
                .withProsecutingAuthority(PROSECUTING_AUTHORITY)
                .withCosts(COSTS)
                .withPostingDate(postingDate)
                .addDefendantDetail(defendantDetail)
                .withCreatedOn(caseCreatedOn);

        for (final UUID materialId : materialIds) {
            caseDetailBuilder.addCaseDocument(getCaseDocument(caseId, materialId));
        }

        return caseDetailBuilder.build();
    }

    private CaseDocument getCaseDocument(final UUID caseId, final UUID materialId) {
        return new CaseDocument(randomUUID(), materialId, "SJPN", clock.now(), caseId, 1);
    }

    private static String randomUrn() {
        return PROSECUTING_AUTHORITY_PREFIX + RandomGenerator.integer(100000000, 999999999).next();
    }

}
