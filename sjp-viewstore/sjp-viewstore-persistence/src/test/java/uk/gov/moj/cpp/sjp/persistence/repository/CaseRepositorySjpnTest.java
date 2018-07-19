package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.time.LocalDate.now;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;

import uk.gov.justice.services.common.util.Clock;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetailMissingSjpn;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDocument;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class CaseRepositorySjpnTest extends BaseTransactionalTest {

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private Clock clock;

    private List<CaseDetail> sjpCasesWithSjpn;
    private List<CaseDetail> uncompletedSjpCasesWithSjpn;
    private List<CaseDetail> uncompletedSjpCasesWithoutSjpn;
    private List<CaseDetail> completedSjpCasesWithoutSjpn;

    @Before
    public void addCasesAndDocuments() {

        final List<CaseDetail> sjpCases = createCases(8);

        sjpCasesWithSjpn = sjpCases.subList(0, 3);
        createSjpNotices(sjpCasesWithSjpn);
        final List<CaseDetail> completedCasesWithSjpn = sjpCasesWithSjpn.subList(0, 1);
        completeCases(completedCasesWithSjpn);

        uncompletedSjpCasesWithSjpn = sjpCasesWithSjpn.subList(completedCasesWithSjpn.size(), sjpCasesWithSjpn.size());

        final List<CaseDetail> sjpCasesWithoutSjpn = sjpCases.subList(sjpCasesWithSjpn.size(), sjpCases.size());
        createOtherDocuments(sjpCasesWithoutSjpn);

        completedSjpCasesWithoutSjpn = sjpCasesWithoutSjpn.subList(0, 1);
        completeCases(completedSjpCasesWithoutSjpn);

        uncompletedSjpCasesWithoutSjpn = sjpCasesWithoutSjpn.subList(1, sjpCasesWithoutSjpn.size());
    }

    @Test
    public void findCasesMissingSjpn() {
        final List<CaseDetail> actualCases = caseRepository.findCasesMissingSjpn().getResultList();
        final List<UUID> actualCaseIds = extractCaseIds(actualCases);

        assertThat(actualCaseIds, equalTo(extractCaseIds(uncompletedSjpCasesWithoutSjpn)));
    }

    @Test
    public void findCasesMissingSjpnWithDetails() {
        final List<CaseDetailMissingSjpn> actualCases = caseRepository.findCasesMissingSjpnWithDetails();
        final List<UUID> actualCaseIds = extractCaseIdsFromCaseDetailMissingSjpn(actualCases);

        assertThat(actualCaseIds, equalTo(extractCaseIds(uncompletedSjpCasesWithoutSjpn)));
    }

    @Test
    public void findCasesMissingSjpnWithLimit() {
        int limit = 3;

        final List<CaseDetail> actualCases = caseRepository.findCasesMissingSjpn().maxResults(limit).getResultList();
        final List<UUID> actualCaseIds = extractCaseIds(actualCases);

        assertThat(actualCaseIds, hasSize(limit));
        assertThat(actualCaseIds, everyItem(isIn(extractCaseIds(uncompletedSjpCasesWithoutSjpn))));
        assertThat(Collections.disjoint(actualCaseIds, extractCaseIds(sjpCasesWithSjpn)), is(true));
        assertThat(Collections.disjoint(actualCaseIds, extractCaseIds(completedSjpCasesWithoutSjpn)), is(true));
    }

    @Test
    public void countCasesMissingSjpn() {
        final int actualCaseCount = caseRepository.countCasesMissingSjpn();

        assertThat(actualCaseCount, equalTo(uncompletedSjpCasesWithoutSjpn.size()));
    }

    @Test
    public void countCasesMissingSjpnWithPostingDateOlderThanSpecified() {
        int sjpCasesMissingSjpnCount = uncompletedSjpCasesWithoutSjpn.size();
        for (int i = 0; i < sjpCasesMissingSjpnCount; i++) {
            final LocalDate postingDate = LocalDate.now().minusDays(i);
            final int actualCaseCount = caseRepository.countCasesMissingSjpn(postingDate);
            assertThat(actualCaseCount, equalTo(sjpCasesMissingSjpnCount - i));
        }
    }

    @Test
    public void findAwaitingSjpCases() {

        final int limit = 1;
        final List<CaseDetail> readySjpCases = caseRepository.findAwaitingSjpCases(limit);

        assertThat(readySjpCases, hasSize(limit));
        assertThat(readySjpCases, equalTo(uncompletedSjpCasesWithSjpn.subList(0, 1)));

    }

    private List<CaseDetail> createCases(final int count) {

        final List<CaseDetail> cases = Stream.generate(CaseDetail::new)
                .limit(count).collect(Collectors.toList());

        int i = cases.size();
        for (CaseDetail caseDetail : cases) {
            caseDetail.setId(UUID.randomUUID());
            caseDetail.setProsecutingAuthority(TFL);
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
            final CaseDocument sjpNotice = new CaseDocument(UUID.randomUUID(), UUID.randomUUID(), documentType, clock.now(), caseDetail.getId(), 1);
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

}
