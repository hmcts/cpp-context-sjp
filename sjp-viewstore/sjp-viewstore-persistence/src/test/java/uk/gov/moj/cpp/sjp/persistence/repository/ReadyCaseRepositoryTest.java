package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PIA;
import static uk.gov.moj.cpp.sjp.domain.CaseReadinessReason.PLEADED_GUILTY;

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

        final ReadyCase readyCase1 = new ReadyCase(UUID.randomUUID(), PIA);
        final ReadyCase readyCase2 = new ReadyCase(UUID.randomUUID(), PIA, assigneeId1);
        final ReadyCase readyCase3 = new ReadyCase(UUID.randomUUID(), PLEADED_GUILTY, assigneeId1);
        final ReadyCase readyCase4 = new ReadyCase(UUID.randomUUID(), PIA, assigneeId2);

        readyCaseRepository.save(readyCase1);
        readyCaseRepository.save(readyCase2);
        readyCaseRepository.save(readyCase3);
        readyCaseRepository.save(readyCase4);

        assertThat(readyCaseRepository.findByAssigneeId(assigneeId1), containsInAnyOrder(readyCase2, readyCase3));
        assertThat(readyCaseRepository.findByAssigneeId(assigneeId2), containsInAnyOrder(readyCase4));
        assertThat(readyCaseRepository.findByAssigneeId(assigneeId3), hasSize(0));
    }
}
