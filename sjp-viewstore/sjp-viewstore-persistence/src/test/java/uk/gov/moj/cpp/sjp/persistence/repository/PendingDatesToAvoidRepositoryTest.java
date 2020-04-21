package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.builder.DatesToAvoidTestData;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PendingDatesToAvoid;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;


@RunWith(CdiTestRunner.class)
public class PendingDatesToAvoidRepositoryTest extends BaseTransactionalTest {

    @Inject
    private PendingDatesToAvoidRepository pendingDatesToAvoidRepository;

    @Inject
    private CaseRepository caseRepository;

    private List<DatesToAvoidTestData> testData;

    @Before
    public void set() {
        testData = Arrays.asList(
                //TFL combinations
                new DatesToAvoidTestData("TFL", null, false, false,
                        ZonedDateTime.now()),
                new DatesToAvoidTestData("TFL", null, true, false,
                        ZonedDateTime.now()),
                new DatesToAvoidTestData("TFL", null, false, true,
                        ZonedDateTime.now()),
                new DatesToAvoidTestData("TFL", "Witness cannot do Thursdays", false, false,
                        ZonedDateTime.now()),
                new DatesToAvoidTestData("TFL", null, false, false,
                        ZonedDateTime.now().minusDays(3)),
                new DatesToAvoidTestData("TFL", null, false, false,
                        ZonedDateTime.now().minusDays(1)),

                //TVL combinations
                new DatesToAvoidTestData("TVL", null, false, false,
                        ZonedDateTime.now()),
                new DatesToAvoidTestData("TVL", null, true, false,
                        ZonedDateTime.now()),
                new DatesToAvoidTestData("TVL", null, false, true,
                        ZonedDateTime.now()),
                new DatesToAvoidTestData("TVL", "Witness cannot do Thursdays", false, false,
                        ZonedDateTime.now()),
                new DatesToAvoidTestData("TVL", null, false, false,
                        ZonedDateTime.now().minusDays(5)),
                new DatesToAvoidTestData("TVL", null, false, false,
                        ZonedDateTime.now().minusDays(8))
        );
        testData.forEach(this::setupAndSaveDatesToAvoidData);
    }

    private void setupAndSaveDatesToAvoidData(DatesToAvoidTestData testData) {
        CaseDetail caseDetail = new CaseDetail();
        caseDetail.setId(testData.getCaseId());
        caseDetail.setProsecutingAuthority(testData.getProsecutingAuthority());
        caseDetail.setDatesToAvoid(testData.getPreviouslySubmittedDatesToAvoid());
        if (testData.isInSession()) {
            caseDetail.setAssigneeId(UUID.randomUUID());
        }
        caseDetail.setCompleted(testData.isCompleted());
        caseRepository.save(caseDetail);
        pendingDatesToAvoidRepository.save(new PendingDatesToAvoid(caseDetail.getId(), testData.getPleaDate()));
    }

    @Test
    public void shouldFindCasesPendingDatesToAvoidForTfl() {
        // WHEN
        List<PendingDatesToAvoid> results = pendingDatesToAvoidRepository.findCasesPendingDatesToAvoid("TFL");

        // THEN
        assertThat(results, hasSize(3));
        assertThat(results.get(0).getCaseId(), equalTo(testData.get(4).getCaseId()));
        assertThat(results.get(1).getCaseId(), equalTo(testData.get(5).getCaseId()));
        assertThat(results.get(2).getCaseId(), equalTo(testData.get(0).getCaseId()));
    }

    @Test
    public void shouldFindCasesPendingDatesToAvoidForTvl() {
        // WHEN
        List<PendingDatesToAvoid> results = pendingDatesToAvoidRepository.findCasesPendingDatesToAvoid("TVL");

        // THEN
        assertThat(results, hasSize(3));
        assertThat(results.get(0).getCaseId(), equalTo(testData.get(11).getCaseId()));
        assertThat(results.get(1).getCaseId(), equalTo(testData.get(10).getCaseId()));
        assertThat(results.get(2).getCaseId(), equalTo(testData.get(6).getCaseId()));
    }

    @Test
    public void shouldRemovePendingDatesToAvoid() {
        List<PendingDatesToAvoid> results = pendingDatesToAvoidRepository.findCasesPendingDatesToAvoid("TFL");
        assertThat(results, hasSize(3));

        pendingDatesToAvoidRepository.removeByCaseId(testData.get(0).getCaseId());

        results = pendingDatesToAvoidRepository.findCasesPendingDatesToAvoid("TFL");
        assertThat(results, hasSize(2));
        assertThat(results.get(0).getCaseId(), equalTo(testData.get(4).getCaseId()));
        assertThat(results.get(1).getCaseId(), equalTo(testData.get(5).getCaseId()));
    }
}