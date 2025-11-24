package uk.gov.moj.cpp.sjp.persistence.repository;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalTest;
import uk.gov.moj.cpp.sjp.persistence.entity.DefendantDetailUpdateRequest;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import junit.framework.TestCase;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class DefendantDetailUpdateRequestRepositoryTest extends BaseTransactionalTest {

    @Inject
    private DefendantDetailUpdateRequestRepository defendantRepository;

    @Test
    public void shouldFindByCaseId() {
        final UUID caseId = UUID.randomUUID();
        DefendantDetailUpdateRequest defendantDetailUpdateRequest = createDefendantDetailUpdateRequest(caseId, UUID.randomUUID(), "firstName", "lastName", "middleName");
        defendantRepository.save(defendantDetailUpdateRequest);

        final DefendantDetailUpdateRequest updateRequest = defendantRepository.findBy(caseId);

        assertThat(caseId, equalTo(updateRequest.getCaseId()));
    }

    private static DefendantDetailUpdateRequest createDefendantDetailUpdateRequest(final UUID caseId, final UUID defendantId, final String firstName, final String lastName, final String middleName) {
        final DefendantDetailUpdateRequest defendantDetailUpdateRequest = new DefendantDetailUpdateRequest();
        defendantDetailUpdateRequest.setCaseId(caseId);
        defendantDetailUpdateRequest.setDefendantId(defendantId);
        defendantDetailUpdateRequest.setFirstName(firstName);
        defendantDetailUpdateRequest.setLastName(lastName);
        defendantDetailUpdateRequest.setStatus(DefendantDetailUpdateRequest.Status.PENDING);
        defendantDetailUpdateRequest.setUpdatedAt(ZonedDateTime.now());

        return defendantDetailUpdateRequest;
    }
}