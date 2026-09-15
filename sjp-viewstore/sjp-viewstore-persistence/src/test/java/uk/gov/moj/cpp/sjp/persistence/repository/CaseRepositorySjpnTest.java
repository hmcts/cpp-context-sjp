package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;
import static org.apache.commons.collections.ListUtils.union;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.common.util.UtcClock;
import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetailMissingSjpn;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CaseRepositorySjpnTest {

    private static final String TFL_PROSECUTOR_FILTER_VALUE = "TFL";
    private static final String COURT_ADMIN_FILTER_VALUE = "%";
    private static final int NUMBER_OF_PROSECUTING_AUTHORITIES = 2;

    private static final String PERSISTENCE_UNIT = "sjp-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider = new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private CaseRepository caseRepository;

    private final Clock clock = new UtcClock();

    private SjpCases tflCases, tvlCases, allCases = new SjpCases();

    @BeforeEach
    void createRepositoryAndSeedCases() {

        caseRepository = new CaseRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(caseRepository);

        tflCases = createCasesAndDocuments("TFL");
        tvlCases = createCasesAndDocuments("TVL");

        allCases.sjpCasesWithSjpn = union(tflCases.sjpCasesWithSjpn, tvlCases.sjpCasesWithSjpn);

        allCases.uncompletedSjpCasesWithSjpn = union(tflCases.uncompletedSjpCasesWithSjpn,
                tvlCases.uncompletedSjpCasesWithSjpn);

        allCases.uncompletedSjpCasesWithoutSjpn = union(tflCases.uncompletedSjpCasesWithoutSjpn,
                tvlCases.uncompletedSjpCasesWithoutSjpn);

        allCases.completedSjpCasesWithoutSjpn = union(tflCases.completedSjpCasesWithoutSjpn,
                tvlCases.completedSjpCasesWithoutSjpn);
    }

    @Test
    void findCasesMissingSjpnForTflProsecutors() {
        final List<CaseDetail> actualCases = caseRepository.findCasesMissingSjpn(TFL_PROSECUTOR_FILTER_VALUE, Collections.emptyList()).getResultList();
        final List<UUID> actualCaseIds = extractCaseIds(actualCases);

        assertThat(actualCaseIds, equalTo(extractCaseIds(tflCases.uncompletedSjpCasesWithoutSjpn)));
    }

    @Test
    void findCasesMissingSjpnForCourtAdmin() {
        final List<CaseDetail> actualCases = caseRepository.findCasesMissingSjpn(COURT_ADMIN_FILTER_VALUE, Collections.emptyList()).getResultList();
        final List<UUID> actualCaseIds = extractCaseIds(actualCases);

        assertThat(actualCaseIds, equalTo(extractCaseIds(allCases.uncompletedSjpCasesWithoutSjpn)));
    }

    @Test
    void findCasesMissingSjpnWithLimitForTflProsecutors() {
        int limit = 3;

        final List<CaseDetail> actualCases = caseRepository.findCasesMissingSjpn(TFL_PROSECUTOR_FILTER_VALUE, Collections.emptyList()).setMaxResults(limit).getResultList();
        final List<UUID> actualCaseIds = extractCaseIds(actualCases);

        assertThat(actualCaseIds, hasSize(limit));
        assertThat(actualCaseIds, everyItem(isIn(extractCaseIds(tflCases.uncompletedSjpCasesWithoutSjpn))));
        assertThat(Collections.disjoint(actualCaseIds, extractCaseIds(tflCases.sjpCasesWithSjpn)), is(true));
        assertThat(Collections.disjoint(actualCaseIds, extractCaseIds(tflCases.completedSjpCasesWithoutSjpn)), is(true));
    }

    @Test
    void findCasesMissingSjpnWithLimitForCourtAdmin() {
        int limit = 3;

        final List<CaseDetail> actualCases = caseRepository.findCasesMissingSjpn(COURT_ADMIN_FILTER_VALUE, Collections.emptyList()).setMaxResults(limit).getResultList();
        final List<UUID> actualCaseIds = extractCaseIds(actualCases);

        assertThat(actualCaseIds, hasSize(limit));
        assertThat(actualCaseIds, everyItem(isIn(extractCaseIds(allCases.uncompletedSjpCasesWithoutSjpn))));
        assertThat(Collections.disjoint(actualCaseIds, extractCaseIds(allCases.sjpCasesWithSjpn)), is(true));
        assertThat(Collections.disjoint(actualCaseIds, extractCaseIds(allCases.completedSjpCasesWithoutSjpn)), is(true));
    }

    @Test
    void countCasesMissingSjpnForTflProsecutors() {
        final int actualCaseCount = caseRepository.countCasesMissingSjpn(TFL_PROSECUTOR_FILTER_VALUE, Collections.emptyList());

        assertThat(actualCaseCount, equalTo(tflCases.uncompletedSjpCasesWithoutSjpn.size()));
    }

    @Test
    void countCasesMissingSjpnForCourAdmin() {
        final int actualCaseCount = caseRepository.countCasesMissingSjpn(COURT_ADMIN_FILTER_VALUE, Collections.emptyList());

        assertThat(actualCaseCount, equalTo(allCases.uncompletedSjpCasesWithoutSjpn.size()));
    }

    @Test
    void countCasesMissingSjpnWithPostingDateOlderThanSpecifiedForTflProsecutors() {
        int sjpCasesMissingSjpnCount = tflCases.uncompletedSjpCasesWithoutSjpn.size();
        for (int i = 0; i < sjpCasesMissingSjpnCount; i++) {
            final LocalDate postingDate = LocalDate.now().minusDays(i);
            final int actualCaseCount = caseRepository.countCasesMissingSjpn(TFL_PROSECUTOR_FILTER_VALUE, postingDate, Collections.emptyList());
            assertThat(actualCaseCount, equalTo(sjpCasesMissingSjpnCount - i));
        }
    }

    @Test
    void countCasesMissingSjpnWithPostingDateOlderThanSpecifiedForCourtAdmin() {
        int sjpCasesMissingSjpnCount = allCases.uncompletedSjpCasesWithoutSjpn.size();
        for (int i = 0; i < sjpCasesMissingSjpnCount / NUMBER_OF_PROSECUTING_AUTHORITIES; i++) {
            final LocalDate postingDate = LocalDate.now().minusDays(i);
            final int actualCaseCount = caseRepository.countCasesMissingSjpn(COURT_ADMIN_FILTER_VALUE, postingDate, Collections.emptyList());
            assertThat(actualCaseCount, equalTo(sjpCasesMissingSjpnCount - (i * NUMBER_OF_PROSECUTING_AUTHORITIES)));
        }
    }

    private SjpCases createCasesAndDocuments(final String prosecutingAuthority) {

        final SjpCases cases = new SjpCases();

        final List<CaseDetail> sjpCases = createCases(8, prosecutingAuthority);

        cases.sjpCasesWithSjpn = sjpCases.subList(0, 3);
        createSjpNotices(cases.sjpCasesWithSjpn);
        final List<CaseDetail> completedCasesWithSjpn = cases.sjpCasesWithSjpn.subList(0, 1);
        completeCases(completedCasesWithSjpn);

        cases.uncompletedSjpCasesWithSjpn = cases.sjpCasesWithSjpn.subList(completedCasesWithSjpn.size(), cases.sjpCasesWithSjpn.size());

        final List<CaseDetail> sjpCasesWithoutSjpn = sjpCases.subList(cases.sjpCasesWithSjpn.size(), sjpCases.size());
        createOtherDocuments(sjpCasesWithoutSjpn);

        cases.completedSjpCasesWithoutSjpn = sjpCasesWithoutSjpn.subList(0, 1);
        completeCases(cases.completedSjpCasesWithoutSjpn);

        cases.uncompletedSjpCasesWithoutSjpn = sjpCasesWithoutSjpn.subList(1, sjpCasesWithoutSjpn.size());

        return cases;
    }

    private List<CaseDetail> createCases(final int count, final String prosecutingAuthority) {

        final List<CaseDetail> cases = Stream.generate(CaseDetail::new)
                .limit(count).collect(Collectors.toList());

        int i = cases.size();
        for (CaseDetail caseDetail : cases) {
            caseDetail.setId(randomUUID());
            caseDetail.setProsecutingAuthority(prosecutingAuthority);
            caseDetail.setPostingDate(now().minusDays(i--));
            caseRepository.save(caseDetail);
        }

        return cases; // ordered by posting date
    }

    private void createSjpNotices(final List<CaseDetail> cases) {
        createCaseDocuments(cases, "SJPN");
    }

    private void createOtherDocuments(final List<CaseDetail> cases) {
        createCaseDocuments(cases, "Other");
    }

    private void createCaseDocuments(final List<CaseDetail> cases, final String documentType) {
        for (final CaseDetail caseDetail : cases) {
            final CaseDocument sjpNotice = new CaseDocument(randomUUID(), randomUUID(), documentType, clock.now(), caseDetail.getId(), 1);
            caseDetail.addCaseDocuments(sjpNotice);
            caseRepository.save(caseDetail);
        }
    }

    private void completeCases(final List<CaseDetail> cases) {
        cases.forEach(casedDetail -> {
            casedDetail.setCompleted(true);
            caseRepository.save(casedDetail);
        });
    }

    private List<UUID> extractCaseIds(final List<CaseDetail> caseDetails) {
        return caseDetails
                .stream()
                .map(CaseDetail::getId)
                .collect(Collectors.toList());
    }

    private List<UUID> extractCaseIdsFromCaseDetailMissingSjpn(final List<CaseDetailMissingSjpn> caseDetails) {
        return caseDetails
                .stream()
                .map(CaseDetailMissingSjpn::getId)
                .collect(Collectors.toList());
    }

    private class SjpCases {
        List<CaseDetail> sjpCasesWithSjpn;
        List<CaseDetail> uncompletedSjpCasesWithSjpn;
        List<CaseDetail> uncompletedSjpCasesWithoutSjpn;
        List<CaseDetail> completedSjpCasesWithoutSjpn;
    }
}
