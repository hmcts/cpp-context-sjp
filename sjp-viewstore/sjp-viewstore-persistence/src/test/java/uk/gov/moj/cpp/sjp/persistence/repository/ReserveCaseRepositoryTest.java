package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.time.ZonedDateTime.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;


import java.util.ArrayList;
import java.util.UUID;
import javax.inject.Inject;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.builder.CaseDetailBuilder;
import uk.gov.moj.cpp.sjp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetail;
import uk.gov.moj.cpp.sjp.persistence.entity.PersonalDetails;
import uk.gov.moj.cpp.sjp.persistence.entity.ReserveCase;

@RunWith(CdiTestRunner.class)
public class ReserveCaseRepositoryTest extends BaseTransactionalTest {

    @Inject
    private ReserveCaseRepository reserveCaseRepository;

    @Inject
    private CaseRepository caseRepository;

    @Test
    public void shouldSaveUnReservedCaseDetail(){
        final UUID caseId = UUID.randomUUID();

        final CaseDetailBuilder caseDetailBuilder = getCaseDetail(caseId);

        caseRepository.save(caseDetailBuilder.build());

        final CaseDetail loadedCaseDetail = caseRepository.findBy(caseId);

        assertThat(loadedCaseDetail.getReserveCase().size(), equalTo(0));
    }

    @Test
    public void shouldSaveAndLoadWithCaseDetail(){
        final UUID reservedBy = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();

        final CaseDetailBuilder caseDetailBuilder = getCaseDetail(caseId);
        final ReserveCase reserveCase = new ReserveCase(UUID.randomUUID(),
                "ABC1234",
                reservedBy,
                now());
        caseDetailBuilder.withReserveCase(reserveCase);

        caseRepository.save(caseDetailBuilder.build());

        final CaseDetail loadedCaseDetail = caseRepository.findBy(caseId);

        assertThat(loadedCaseDetail.getReserveCase().get(0), equalTo(reserveCase));
    }

    @Test
    public void shouldDeleteAndLoadWithCaseDetail(){
        final UUID reservedBy = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();

        final CaseDetailBuilder caseDetailBuilder = getCaseDetail(caseId);
        final ReserveCase reserveCase = new ReserveCase(UUID.randomUUID(),
                "ABC1234",
                reservedBy,
                now());
        caseDetailBuilder.withReserveCase(reserveCase);
        final CaseDetail caseDetail = caseDetailBuilder.build();
        caseRepository.save(caseDetail);

        final CaseDetail loadedCaseDetail = caseRepository.findBy(caseId);

        assertThat(loadedCaseDetail.getReserveCase().get(0), equalTo(reserveCase));

        caseDetail.setReserveCase(new ArrayList<>());
        caseRepository.save(caseDetail);
        final CaseDetail loadedCaseDetail2 = caseRepository.findBy(caseId);

        assertThat(loadedCaseDetail2.getReserveCase().size(), equalTo(0));

    }

    private CaseDetailBuilder getCaseDetail(final UUID caseId){
        final DefendantDetail defendantDetail = new DefendantDetail(UUID.randomUUID(), new PersonalDetails(), null,1,null,null,null);
        return CaseDetailBuilder.aCase().withCaseId(caseId).withDefendantDetail(defendantDetail);

    }
}
