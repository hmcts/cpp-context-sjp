package uk.gov.moj.cpp.sjp.persistence.repository;

import static java.time.LocalDate.now;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.DVLA;
import static uk.gov.moj.cpp.sjp.domain.ProsecutingAuthority.TFL;
import static uk.gov.moj.cpp.sjp.domain.SessionType.DELEGATED_POWERS;
import static uk.gov.moj.cpp.sjp.domain.SessionType.MAGISTRATE;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.ReadyCase;

import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class ReadyCaseRepositoryTest extends BaseTransactionalTest {

    @Inject
    private ReadyCaseRepository readyCaseRepository;

    @Test
    public void shouldGetReadyCasesByAssigneeId() {
        final UUID assigneeId1 = UUID.randomUUID();
        final UUID assigneeId2 = UUID.randomUUID();
        final UUID assigneeId3 = UUID.randomUUID();

        final ReadyCase readyCase1 = new ReadyCase(UUID.randomUUID(), PIA, null, MAGISTRATE, 3, TFL, now().minusDays(30));
        final ReadyCase readyCase2 = new ReadyCase(UUID.randomUUID(), PIA, assigneeId1, MAGISTRATE, 3, TFL, now().minusDays(30));
        final ReadyCase readyCase3 = new ReadyCase(UUID.randomUUID(), PLEADED_GUILTY, assigneeId1, MAGISTRATE, 2, TFL, now().minusDays(15));
        final ReadyCase readyCase4 = new ReadyCase(UUID.randomUUID(), PIA, assigneeId2, MAGISTRATE, 3, TFL, now().minusDays(30));

        readyCaseRepository.save(readyCase1);
        readyCaseRepository.save(readyCase2);
        readyCaseRepository.save(readyCase3);
        readyCaseRepository.save(readyCase4);

        assertThat(readyCaseRepository.findByAssigneeId(assigneeId1), containsInAnyOrder(readyCase2, readyCase3));
        assertThat(readyCaseRepository.findByAssigneeId(assigneeId2), containsInAnyOrder(readyCase4));
        assertThat(readyCaseRepository.findByAssigneeId(assigneeId3), hasSize(0));
    }

    @Test
    public void shouldSaveAndLoadAllFields() {
        final UUID assigneeId = UUID.randomUUID();
        final UUID caseId = UUID.randomUUID();

        // save a case
        final ReadyCase readyCase = new ReadyCase(caseId,
                PLEADED_GUILTY,
                assigneeId,
                DELEGATED_POWERS,
                1,
                DVLA,
                now());

        // when
        readyCaseRepository.save(readyCase);

        // then
        final ReadyCase loadedReadyCase = readyCaseRepository.findBy(caseId);
        assertThat(loadedReadyCase, equalTo(readyCase));
        assertThat(loadedReadyCase.getReason(), equalTo(PLEADED_GUILTY));
        assertThat(loadedReadyCase.getAssigneeId().get(), equalTo(assigneeId));
        assertThat(loadedReadyCase.getSessionType(), equalTo(DELEGATED_POWERS));
        assertThat(loadedReadyCase.getPriority(), equalTo(1));
        assertThat(loadedReadyCase.getProsecutionAuthority(), equalTo(DVLA));
        assertThat(loadedReadyCase.getPostingDate(), notNullValue());
    }
}
