package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.core.random.RandomGenerator;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.sjp.domain.CaseReadinessReason;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseWithoutDefendantPostcode;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.OffenceDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingCaseToPublishPerOffence;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NonUniqueResultException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CaseRepositoryTest {

    private static final Map<UUID, CaseDetail> CASES = new HashMap<>();

    private static final String PROSECUTING_AUTHORITY = "TFL";

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
    private static final String POSTCODE_1 = "CR0 1AB";
    private static final String POSTCODE_2 = "BH7 1AB";
    private static final String OFFENCE_CODE = "PS0001";
    private static final List<ReadyCase> READY_CASES = new ArrayList<>();
    private static LocalDate postingDate = LocalDate.of(2015, 12, 31);

    private static final String PERSISTENCE_UNIT = "sjp-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider provider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private EntityManager entityManager;

    private CaseRepository caseRepository;

    private ReadyCaseRepository readyCaseRepository;

    private final Clock clock = new UtcClock();

    private ZonedDateTime caseCreatedOn;

    private CaseDetail case1, case2, case3, case4;

    private static String randomUrn() {
        return PROSECUTING_AUTHORITY + RandomGenerator.integer(100000000, 999999999).next();
    }

    @BeforeEach
    void createRepositoriesAndSeedCases() {
        caseRepository = new CaseRepository();
        provider.injectEntityManagerInto(caseRepository);

        readyCaseRepository = new ReadyCaseRepository();
        provider.injectEntityManagerInto(readyCaseRepository);

        entityManager = provider.getEntityManager();

        caseCreatedOn = clock.now();
        // given 4 cases exist in database
        case1 = getCase(VALID_CASE_ID_1, VALID_URN_1, VALID_DEFENDANT_ID_1);
        case1.setEnterpriseId(ENTERPRISE_ID);
        // case 2 is withdrawn
        case2 = getCase(VALID_CASE_ID_2, VALID_URN_2, VALID_DEFENDANT_ID_2, true, POSTCODE_1, VALID_MATERIAL_ID_1, VALID_MATERIAL_ID_2);
        case3 = getCase(VALID_CASE_ID_3, VALID_URN_3);
        case4 = getCase(VALID_CASE_ID_4, VALID_URN_4, VALID_DEFENDANT_ID_4);

        CASES.put(VALID_CASE_ID_1, case1);
        CASES.put(VALID_CASE_ID_2, case2);
        CASES.put(VALID_CASE_ID_3, case3);
        CASES.put(VALID_CASE_ID_4, case4);

        CASES.values().forEach(caseRepository::save);

        // test duplicate cases aren't possible
        caseRepository.save(case1);

        READY_CASES.add(new ReadyCase(case1.getId(), CaseReadinessReason.PIA, null, MAGISTRATE, 3, "TFL", now().minusDays(30), now()));
        READY_CASES.add(new ReadyCase(case2.getId(), CaseReadinessReason.WITHDRAWAL_REQUESTED, null, DELEGATED_POWERS, 1, "TFL", now().minusDays(15), now()));
        READY_CASES.add(new ReadyCase(case3.getId(), CaseReadinessReason.PLEADED_GUILTY, null, MAGISTRATE, 2, "TFL", now().minusDays(30), now()));
        // leave case 4 as not ready

        READY_CASES.forEach(readyCaseRepository::save);

        //flush pending inserts then clear L1 cache to force JPA to execute query against database
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldFindPressTransparencyReportPendingCases() {
        final List<PendingCaseToPublishPerOffence> caseDetails = caseRepository.findPressTransparencyReportPendingCases();
        assertNotNull(caseDetails);
    }

    @Test
    void shouldFindPressTransparencyDeltaReportPendingCases() {
        final List<PendingCaseToPublishPerOffence> caseDetails = caseRepository.findPressTransparencyDeltaReportPendingCases(any(), any());
        assertNotNull(caseDetails);
    }

    @Test
    void shouldFindPublicTransparencyReportPendingCases() {
        final List<PendingCaseToPublishPerOffence> caseDetails = caseRepository.findPublicTransparencyReportPendingCases();
        assertNotNull(caseDetails);
    }

    @Test
    void shouldFindPublicTransparencyDeltaReportPendingCases() {
        final List<PendingCaseToPublishPerOffence> caseDetails = caseRepository.findPublicTransparencyDeltaReportPendingCases(any(), any());
        assertNotNull(caseDetails);
    }

    @Test
    void shouldFindCase() {
        final CaseDetail actualCase = caseRepository.findBy(VALID_CASE_ID_1);
        assertNotNull(actualCase);
    }

    @Test
    void shouldFindCaseWithMultipleDocuments() {
        final CaseDetail actualCase = caseRepository.findBy(VALID_CASE_ID_2);
        assertNotNull(actualCase);
        assertThat(actualCase.getCaseDocuments(), hasSize(case2.getCaseDocuments().size()));
        assertThat(actualCase.getCaseSearchResults(), hasSize(case2.getCaseSearchResults().size()));
        assertThat(actualCase.getDefendant().getOffences(), hasSize(case2.getDefendant().getOffences().size()));
    }

    @Test
    void shouldFindCaseMatchingUrn() {
        final CaseDetail actualCase = caseRepository.findByUrn(VALID_URN_1);
        assertNotNull(actualCase);
        assertEquals(VALID_CASE_ID_1, actualCase.getId(), "ID should match ID of case 1");
        assertThat(caseCreatedOn, is(actualCase.getDateTimeCreated()));
    }

    @Test
    void shouldFindCaseMatchingUrnIgnoringCase() {
        final CaseDetail actualCase = caseRepository.findByUrn(VALID_URN_1.toLowerCase());
        assertNotNull(actualCase);
        assertEquals(VALID_CASE_ID_1, actualCase.getId(), "ID should match ID of case 1");
    }

    @Test
    void shouldFindCaseByPersonId() {
        final List<CaseDetail> caseDetails = caseRepository.findByDefendantId(VALID_DEFENDANT_ID_1);
        assertNotNull(caseDetails);
        assertThat("Should have 1 entry", caseDetails, hasSize(1));
        assertEquals(VALID_CASE_ID_1, caseDetails.get(0).getId(), "ID should match ID of case 1");
    }

    @Test
    void shouldFindCaseWithEnterpriseIdByCaseId() {
        final CaseDetail caseDetail = caseRepository.findBy(VALID_CASE_ID_1);

        assertThat(caseDetail.getEnterpriseId(), equalTo(ENTERPRISE_ID));
    }

    @Test
    void shouldFindCaseDocuments() {
        final List<CaseDocument> caseDocuments = caseRepository.findCaseDocuments(VALID_CASE_ID_2);
        assertNotNull(caseDocuments);
        assertThat(caseDocuments.stream().map(CaseDocument::getMaterialId).collect(toList()), containsInAnyOrder(VALID_MATERIAL_ID_1, VALID_MATERIAL_ID_2));
    }

    @Test
    void shouldFindCaseDefendants_Success() {
        final DefendantDetail defendant = caseRepository.findCaseDefendant(VALID_CASE_ID_3);
        assertNotNull(defendant);
        assertEquals(VALID_CASE_ID_3, defendant.getCaseDetail().getId());
    }

    @Test
    void shouldCompleteCaseSuccessfully() {
        assertFalse(CASES.get(VALID_CASE_ID_1).isCompleted(),
                "CaseAggregate should not be completed");

        caseRepository.completeCase(VALID_CASE_ID_1);
        final CaseDetail actualCase = caseRepository.findBy(VALID_CASE_ID_1);
        assertTrue(actualCase.isCompleted(), "CaseAggregate should be completed");
        assertNull(actualCase.getAdjournedTo());
    }

    @Test
    void shouldUpdateLibraCaseReopenedDetails() {
        final LocalDate reopenedDate = now();
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
    void shouldFindCaseByMaterialIdWhenMaterialIsDocument() {

        final CaseDetail actualCase = caseRepository.findByUrn(VALID_URN_2);

        final CaseDetail caseReturned = caseRepository.findByMaterialId(VALID_MATERIAL_ID_1);

        assertThat(caseReturned.getId(), is(actualCase.getId()));
    }

    @Test
    void shouldPersistCurrencyAndOtherSupportingInformation() {
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
    void shouldNotFindNonExistingCase() {
        final CaseDetail caseDetail = caseRepository.findBy(UUID.randomUUID());

        assertThat(caseDetail, nullValue());
    }

    @Test
    void shouldFindCaseMatchingUrnWithPrefixAndPostcode() {
        final CaseDetail actualCase = caseRepository.findByUrnPostcode(VALID_URN_1, POSTCODE_1);

        assertNotNull(actualCase);
        assertEquals(VALID_CASE_ID_1, actualCase.getId(), "ID should match ID of case 1");
        assertEquals(VALID_URN_1, actualCase.getUrn(), "URN should match URN of case 1");

    }

    @Test
    void shouldFindCaseMatchingUrnWithPrefixAndPostcodeWithExtraSpaces() {
        final CaseDetail actualCase = caseRepository.findByUrnPostcode(VALID_URN_1, String.format("  %s   ", POSTCODE_1));

        assertNotNull(actualCase);
        assertEquals(VALID_CASE_ID_1, actualCase.getId(), "ID should match ID of case 1");
        assertEquals(VALID_URN_1, actualCase.getUrn(), "URN should match URN of case 1");

    }

    @Test
    void shouldFindCaseMatchingUrnWithoutPrefixAndPostcode() {
        final CaseDetail actualCase = caseRepository.findByUrnPostcode(VALID_URN_1.replace(PROSECUTING_AUTHORITY, ""), POSTCODE_1);

        assertNotNull(actualCase);
        assertEquals(VALID_CASE_ID_1, actualCase.getId(), "ID should match ID of case 1");
        assertEquals(VALID_URN_1, actualCase.getUrn(), "URN should match URN of case 1");

    }

    @Test
    void shouldFindCaseWhenUrnWithoutPrefixSameButPostcodeDifferent() {

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

    @Test
    void shouldThrowExceptionWhenTwoCasesHaveSameUrnWithoutPrefixAndPostcode() {

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

        //when / then throws exception
        assertThrows(NonUniqueResultException.class, () -> caseRepository.findByUrnPostcode("12345678A", postcode1));
    }

    @Test
    void shouldFindCasesForSOCCheck() {
        final String loggedInUserId = "2781b565-4514-4805-8744-a3e827f0f611";
        final String ljaCode = "2577";
        final String courtHouseCode = "B01LY00";
        final LocalDate fromDate = LocalDate.of(2020, 01, 01);
        final LocalDate toDate = LocalDate.now();
        final String sortField = "magistrate";
        final String sortOrder = "asc";
        List<Object[]> cases = caseRepository.findCasesForSOCCheck(loggedInUserId, ljaCode, courtHouseCode, fromDate, toDate, sortField, sortOrder);
        assertNotNull(cases);
    }

    @Test
    void shouldFindCasesWithoutDefendantPostCode() {
        //given
        final CaseDetail caseDetail1 = getCase(randomUUID(), VALID_URN_1, POSTCODE_1);
        final CaseDetail caseDetail2 = getCase(randomUUID(), VALID_URN_2, POSTCODE_2);

        caseRepository.save(caseDetail1);
        caseRepository.save(caseDetail2);
        caseDetail1.setCompleted(false);
        caseDetail1.getDefendant().getAddress().setPostcode(null);

        //when
        final List<CaseWithoutDefendantPostcode> casesWithoutDefendantPostcode = caseRepository.findCasesWithoutDefendantPostcode();

        //then
        assertEquals(VALID_URN_1, casesWithoutDefendantPostcode.get(0).getUrn());
        assertEquals(1, casesWithoutDefendantPostcode.size());
    }

    @Test
    void shouldFindCasesWithoutDefendantPostCodeWhenDefendantIsCompany() {
        //given
        final CaseDetail caseDetail1 = getCase(randomUUID(), VALID_URN_1, POSTCODE_1);
        final CaseDetail caseDetail2 = getCase(randomUUID(), VALID_URN_2, POSTCODE_2);

        caseRepository.save(caseDetail1);
        caseRepository.save(caseDetail2);
        caseDetail1.setCompleted(false);
        caseDetail1.getDefendant().getAddress().setPostcode(null);
        caseDetail1.getDefendant().setPersonalDetails(null);
        caseDetail1.getDefendant().getLegalEntityDetails().setLegalEntityName("LegalEntityName");

        //when
        final List<CaseWithoutDefendantPostcode> casesWithoutDefendantPostcode = caseRepository.findCasesWithoutDefendantPostcode();

        //then
        assertEquals(VALID_URN_1, casesWithoutDefendantPostcode.get(0).getUrn());
        assertEquals("LegalEntityName", casesWithoutDefendantPostcode.get(0).getLegalEntityName());
        assertEquals(1, casesWithoutDefendantPostcode.size());
    }

    private CaseDetail getCase(final UUID caseId, final String urn) {
        return getCase(caseId, urn, randomUUID());
    }

    private CaseDetail getCase(final UUID caseId, final String urn, final String postcode) {
        return getCase(caseId, urn, randomUUID(), false, postcode, randomUUID());
    }

    private CaseDetail getCase(final UUID caseId, final String urn, final UUID defendantId) {
        return getCase(caseId, urn, defendantId, false, POSTCODE_1, randomUUID());
    }

    private CaseDetail getCase(final UUID caseId, final String urn, final UUID defendantId, final boolean withdrawn, final String postcode, final UUID... materialIds) {

        final DefendantDetail defendantDetail = DefendantDetailBuilder.aDefendantDetail()
                .withId(defendantId)
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
                .withDefendantDetail(defendantDetail)
                .withCreatedOn(caseCreatedOn);

        for (final UUID materialId : materialIds) {
            caseDetailBuilder.addCaseDocument(getCaseDocument(caseId, materialId));
        }

        return caseDetailBuilder.build();
    }

    private CaseDocument getCaseDocument(final UUID caseId, final UUID materialId) {
        return new CaseDocument(randomUUID(), materialId, "SJPN", clock.now(), caseId, 1);
    }

}
