package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.sjp.persistence.builder.DefendantDetailBuilder.aDefendantDetail;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.domain.plea.Plea;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.view.CaseCountByAgeView;

import java.time.LocalDate;
import java.util.List;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class NotReadyCaseRepositoryTest extends BaseTransactionalTest {

    @Inject
    private CaseRepository caseRepository;

    @Inject
    private NotReadyCaseRepository notReadyCaseRepository;

    @Test
    public void shouldIncludeOnlyNotReadySjpCases() {
        final LocalDate now = LocalDate.now();
        final CaseDetail notCompletedNonSjpCase = CaseDetailBuilder.aCase()
                .withCompleted(false).withPostingDate(now)
                .addDefendantDetail(aDefendantDetail().build()).build();
        final CaseDetail notCompletedSjpCase = CaseDetailBuilder.aCase()
                .withCompleted(false).withInitiationCode("J").withPostingDate(now.minusDays(1))
                .addDefendantDetail(aDefendantDetail().build()).build();
        final CaseDetail completedNonSjpCase = CaseDetailBuilder.aCase()
                .withCompleted(true).withPostingDate(now.minusDays(2))
                .addDefendantDetail(aDefendantDetail().build()).build();
        final CaseDetail completedSjpCase = CaseDetailBuilder.aCase()
                .withCompleted(true).withInitiationCode("J").withPostingDate(now.minusDays(3))
                .addDefendantDetail(aDefendantDetail().build()).build();
        final CaseDetail withdrawalRequestedSjpCase = CaseDetailBuilder.aCase().withInitiationCode("J").withPostingDate(now.minusDays(3))
                .addDefendantDetail(aDefendantDetail().withOffencePendingWithdrawal(true).build()).build();
        final CaseDetail pleaSjpCase = CaseDetailBuilder.aCase().withInitiationCode("J").withPostingDate(now.minusDays(3))
                .addDefendantDetail(aDefendantDetail().withPlea(Plea.Type.GUILTY).build()).build();

        caseRepository.save(notCompletedNonSjpCase);
        caseRepository.save(notCompletedSjpCase);
        caseRepository.save(completedNonSjpCase);
        caseRepository.save(completedSjpCase);
        caseRepository.save(withdrawalRequestedSjpCase);
        caseRepository.save(pleaSjpCase);

        final List<CaseCountByAgeView> casesCountByAge = notReadyCaseRepository.getCountOfCasesByAge();
        assertThat(casesCountByAge, hasSize(is(1)));
        assertThat(casesCountByAge.get(0).getAge(), is(1));
        assertThat(casesCountByAge.get(0).getCount(), is(1));
    }

    @Test
    public void shouldGroupByAge() {
        final LocalDate now = LocalDate.now();
        final CaseDetail case1 = CaseDetailBuilder.aCase().addDefendantDetail(aDefendantDetail().build())
                .withCompleted(false).withInitiationCode("J").withPostingDate(now.minusDays(1)).build();
        final CaseDetail case2 = CaseDetailBuilder.aCase().addDefendantDetail(aDefendantDetail().build())
                .withCompleted(false).withInitiationCode("J").withPostingDate(now.minusDays(2)).build();
        final CaseDetail case3 = CaseDetailBuilder.aCase().addDefendantDetail(aDefendantDetail().build())
                .withCompleted(false).withInitiationCode("J").withPostingDate(now.minusDays(3)).build();
        final CaseDetail case4 = CaseDetailBuilder.aCase().addDefendantDetail(aDefendantDetail().build())
                .withCompleted(false).withInitiationCode("J").withPostingDate(now.minusDays(3)).build();

        caseRepository.save(case1);
        caseRepository.save(case2);
        caseRepository.save(case3);
        caseRepository.save(case4);

        final List<CaseCountByAgeView> casesCountByAge = notReadyCaseRepository.getCountOfCasesByAge();
        assertThat(casesCountByAge, hasSize(is(3)));
        assertThat(casesCountByAge.get(0).getAge(), is(1));
        assertThat(casesCountByAge.get(0).getCount(), is(1));
        assertThat(casesCountByAge.get(1).getAge(), is(2));
        assertThat(casesCountByAge.get(1).getCount(), is(1));
        assertThat(casesCountByAge.get(2).getAge(), is(3));
        assertThat(casesCountByAge.get(2).getCount(), is(2));
    }
}
